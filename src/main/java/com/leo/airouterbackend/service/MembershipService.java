package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.membership.PointPackageSaveRequest;
import com.leo.airouterbackend.model.dto.membership.SubscriptionPlanSaveRequest;
import com.leo.airouterbackend.model.dto.membership.UserSubscriptionAdjustRequest;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.entity.PointPackage;
import com.leo.airouterbackend.model.entity.SubscriptionPlan;
import com.leo.airouterbackend.model.entity.UserSubscription;
import com.leo.airouterbackend.model.vo.MembershipVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface MembershipService extends IService<SubscriptionPlan> {

    List<SubscriptionPlan> listVisiblePlans();

    List<PointPackage> listVisiblePointPackages();

    SubscriptionPlan getActivePlan(String planCode);

    PointPackage getActivePointPackage(String packageCode);

    boolean savePlan(SubscriptionPlanSaveRequest request);

    boolean savePointPackage(PointPackageSaveRequest request);

    UserSubscription getActiveSubscription(Long userId);

    void activateSubscription(PaymentOrder order);

    void adjustUserSubscription(UserSubscriptionAdjustRequest request);

    MembershipVO getMyMembership(Long userId);
}
