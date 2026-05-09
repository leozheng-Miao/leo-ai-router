package com.leo.airouterbackend.mapper;

import com.leo.airouterbackend.model.dto.conversation.ConversationMessagePreviewDTO;
import com.leo.airouterbackend.model.entity.ConversationMessage;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {

    @Select("SELECT COALESCE(MAX(seq), 0) FROM conversation_message WHERE conversation_id = #{conversationId}")
    Long selectMaxSeq(@Param("conversationId") Long conversationId);

    @Select("""
            SELECT *
            FROM (
                SELECT *
                FROM conversation_message
                WHERE conversation_id = #{conversationId}
                  AND role IN ('user', 'assistant')
                ORDER BY seq DESC
                LIMIT #{limit}
            ) t
            ORDER BY seq ASC
            """)
    List<ConversationMessage> selectRecentContext(@Param("conversationId") Long conversationId,
                                                  @Param("limit") int limit);

    @Select("""
            <script>
            SELECT conversation_id AS conversationId, content
            FROM (
                SELECT conversation_id,
                       content,
                       ROW_NUMBER() OVER (PARTITION BY conversation_id ORDER BY seq DESC) AS rn
                FROM conversation_message
                WHERE role = 'assistant'
                  AND conversation_id IN
                  <foreach collection="conversationIds" item="id" open="(" separator="," close=")">
                      #{id}
                  </foreach>
            ) t
            WHERE rn = 1
            </script>
            """)
    List<ConversationMessagePreviewDTO> selectLatestAssistantMessages(@Param("conversationIds") List<Long> conversationIds);
}
