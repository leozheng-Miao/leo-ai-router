package com.leo.airouterbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.ConversationMapper;
import com.leo.airouterbackend.mapper.ConversationMessageMapper;
import com.leo.airouterbackend.model.dto.conversation.ConversationMessagePreviewDTO;
import com.leo.airouterbackend.model.dto.conversation.CreateConversationRequest;
import com.leo.airouterbackend.model.entity.Conversation;
import com.leo.airouterbackend.model.vo.ConversationVO;
import com.leo.airouterbackend.service.ConversationService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    private static final int DEFAULT_CONV_TYPE = 1;
    private static final String DEFAULT_TITLE = "新对话";
    private static final int TITLE_MAX_CODE_POINTS = 20;
    private static final int PREVIEW_MAX_CODE_POINTS = 50;
    private static final Duration LIST_CACHE_TTL = Duration.ofSeconds(30);
    private static final long REDIS_SCAN_COUNT = 100L;
    private static final String CONVERSATION_SEQ_PREFIX = "conv:seq:";

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Long createConversation(Long userId, CreateConversationRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "X-User-Id 不能为空");
        }
        int convType = request == null || request.getConvType() == null ? DEFAULT_CONV_TYPE : request.getConvType();
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .title(DEFAULT_TITLE)
                .convType(convType)
                .lastMessageAt(now)
                .createdAt(now)
                .isDeleted(0)
                .build();
        boolean saved = this.save(conversation);
        if (!saved || conversation.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建会话失败");
        }
        evictUserConversationListCache(userId);
        return conversation.getId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<ConversationVO> listConversations(Long userId, long page, long size) {
        if (userId == null || userId <= 0 || page < 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String cacheKey = buildListCacheKey(userId, page, size);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Page<?>) {
            return (Page<ConversationVO>) cached;
        }

        Page<Conversation> conversationPage = this.page(Page.of(page + 1, size), QueryWrapper.create()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderBy("last_message_at", false));

        List<Conversation> records = conversationPage.getRecords();
        List<Long> conversationIds = records.stream().map(Conversation::getId).toList();
        Map<Long, ConversationMessagePreviewDTO> previewMap = CollUtil.isEmpty(conversationIds)
                ? Map.of()
                : conversationMessageMapper.selectLatestAssistantMessages(conversationIds)
                .stream()
                .collect(Collectors.toMap(ConversationMessagePreviewDTO::getConversationId, Function.identity(), (a, b) -> a));

        List<ConversationVO> voRecords = records.stream()
                .map(conversation -> toConversationVO(conversation, previewMap.get(conversation.getId())))
                .toList();

        Page<ConversationVO> result = new Page<>(page + 1, size, conversationPage.getTotalRow());
        result.setRecords(voRecords);
        redisTemplate.opsForValue().set(cacheKey, result, LIST_CACHE_TTL);
        return result;
    }

    @Override
    public Conversation validateOwner(Long userId, Long conversationId) {
        if (userId == null || userId <= 0 || conversationId == null || conversationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Conversation conversation = this.getOne(QueryWrapper.create()
                .eq("id", conversationId)
                .eq("is_deleted", 0));
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }
        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权访问该会话");
        }
        return conversation;
    }

    @Override
    public void updateAfterMessage(Conversation conversation, String userContent) {
        if (conversation == null || conversation.getId() == null) {
            return;
        }
        conversation.setLastMessageAt(LocalDateTime.now());
        if (DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(StrUtil.blankToDefault(limitCodePoints(userContent, TITLE_MAX_CODE_POINTS), DEFAULT_TITLE));
        }
        boolean updated = this.updateById(conversation);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新会话失败");
        }
        evictUserConversationListCache(conversation.getUserId());
    }

    @Override
    public boolean deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = validateOwner(userId, conversationId);
        conversation.setIsDeleted(1);
        boolean updated = this.updateById(conversation);
        if (updated) {
            stringRedisTemplate.delete(CONVERSATION_SEQ_PREFIX + conversationId);
            evictUserConversationListCache(userId);
        }
        return updated;
    }

    @Override
    public void evictUserConversationListCache(Long userId) {
        if (userId == null) {
            return;
        }
        String pattern = "user:conv:list:" + userId + ":*";
        Set<String> keys = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> scanKeys(connection, pattern));
        if (CollUtil.isNotEmpty(keys)) {
            stringRedisTemplate.delete(keys);
        }
    }

    private Set<String> scanKeys(RedisConnection connection, String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(REDIS_SCAN_COUNT)
                .build();
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.warn("扫描 Redis 会话列表缓存失败，pattern={}", pattern, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除会话列表缓存失败");
        }
        return keys;
    }

    private String buildListCacheKey(Long userId, long page, long size) {
        return "user:conv:list:" + userId + ":" + page + ":" + size;
    }

    private ConversationVO toConversationVO(Conversation conversation, ConversationMessagePreviewDTO preview) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setConvType(conversation.getConvType());
        vo.setLastMessageAt(conversation.getLastMessageAt());
        vo.setLastMessagePreview(preview == null ? "" : limitCodePoints(preview.getContent(), PREVIEW_MAX_CODE_POINTS));
        return vo;
    }

    private String limitCodePoints(String value, int maxCodePoints) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }
}
