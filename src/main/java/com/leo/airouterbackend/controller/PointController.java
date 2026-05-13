package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.model.entity.PointTransaction;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.service.PointService;
import com.leo.airouterbackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/points")
public class PointController {

    @Resource
    private PointService pointService;

    @Resource
    private UserService userService;

    @GetMapping("/transactions/my")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Page<PointTransaction>> listMyTransactions(@RequestParam(defaultValue = "1") long pageNum,
                                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                                   HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pointService.listUserTransactions(loginUser.getId(), pageNum, pageSize));
    }
}
