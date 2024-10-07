/*
 * Copyright (C) 2024 Broadleaf Commerce
 *
 * Licensed under the Broadleaf End User License Agreement (EULA), Version 1.1 (the
 * "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt).
 *
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the
 * "Custom License") between you and Broadleaf Commerce. You may not use this file except in
 * compliance with the applicable license.
 *
 * NOTICE: All information contained herein is, and remains the property of Broadleaf Commerce, LLC
 * The intellectual and technical concepts contained herein are proprietary to Broadleaf Commerce,
 * LLC and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained from Broadleaf Commerce, LLC.
 */
package com.broadleafcommerce.subscriptionoperation.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionCreationRequestException;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionItemCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionUpgradeRequest;

import java.util.ArrayList;
import java.util.List;

import cz.jirutka.rsql.parser.ast.Node;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultSubscriptionOperationService<S extends Subscription, I extends SubscriptionItem, SWI extends SubscriptionWithItems>
        implements SubscriptionOperationService<S, I, SWI> {

    @Getter(AccessLevel.PROTECTED)
    protected final SubscriptionProvider<SWI> subscriptionProvider;

    @Getter(AccessLevel.PROTECTED)
    protected final TypeFactory typeFactory;

    @Override
    public Page<SWI> readSubscriptionsForUserTypeAndUserId(@lombok.NonNull String userType,
            @lombok.NonNull String userId,
            @Nullable Pageable page,
            @Nullable Node filters,
            @Nullable ContextInfo contextInfo) {
        return subscriptionProvider.readSubscriptionsForUserTypeAndUserId(userType, userId, page,
                filters, contextInfo);
    }

    @Override
    public SWI createSubscriptionWithItems(
            @lombok.NonNull SubscriptionCreationRequest creationRequest,
            @Nullable ContextInfo contextInfo) {
        validateSubscriptionCreationRequest(creationRequest, contextInfo);

        SWI subscriptionWithItemsToCreate =
                buildSubscriptionWithItems(creationRequest, contextInfo);

        return subscriptionProvider.create(subscriptionWithItemsToCreate, contextInfo);
    }

    @Override
    public S cancelSubscription(
            @lombok.NonNull SubscriptionCancellationRequest cancellationRequest,
            @Nullable ContextInfo context) {
        return null;
    }

    @Override
    public S upgradeSubscription(@lombok.NonNull SubscriptionUpgradeRequest upgradeRequest,
            @Nullable ContextInfo contextInfo) {
        return null;
    }

    protected void validateSubscriptionCreationRequest(
            @lombok.NonNull SubscriptionCreationRequest creationRequest,
            @Nullable ContextInfo contextInfo) {
        if (StringUtils.isBlank(creationRequest.getUserRefType())
                || StringUtils.isBlank(creationRequest.getUserRef())) {
            throw new InvalidSubscriptionCreationRequestException(
                    "A subscription must be given an owning user/account via userRefType and userRef.");
        }
        if (StringUtils.isBlank(creationRequest.getPeriodType())
                && StringUtils.isBlank(creationRequest.getBillingFrequency())) {
            throw new InvalidSubscriptionCreationRequestException(
                    "A subscription must be given a periodType or billingFrequency.");
        }
        if (CollectionUtils.isEmpty(creationRequest.getItemCreationRequests())) {
            throw new InvalidSubscriptionCreationRequestException(
                    "Subscription items must also be defined for the subscription.");
        }
    }

    protected void validateSubscriptionCancellationRequest(
            @lombok.NonNull SubscriptionCancellationRequest cancellationRequest,
            @Nullable ContextInfo contextInfo) {

    }

    protected void validateSubscriptionDowngradeRequest(
            @lombok.NonNull SubscriptionCreationRequest creationRequest,
            @Nullable ContextInfo contextInfo) {

    }


    @SuppressWarnings("unchecked")
    protected SWI buildSubscriptionWithItems(
            @lombok.NonNull SubscriptionCreationRequest creationRequest,
            @Nullable ContextInfo contextInfo) {
        S subscription = buildSubscription(creationRequest, contextInfo);
        List<I> items = buildSubscriptionItems(creationRequest, contextInfo);

        SWI subscriptionWithItemsToBeCreated = (SWI) typeFactory.get(SubscriptionWithItems.class);
        subscriptionWithItemsToBeCreated.setSubscription(subscription);
        subscriptionWithItemsToBeCreated.setSubscriptionItems((List<SubscriptionItem>) items);

        return subscriptionWithItemsToBeCreated;
    }

    @SuppressWarnings("unchecked")
    protected S buildSubscription(@lombok.NonNull SubscriptionCreationRequest request,
            @Nullable ContextInfo contextInfo) {
        S subscription = (S) typeFactory.get(Subscription.class);
        subscription.setName(request.getName());
        subscription.setSubscriptionStatus(request.getSubscriptionStatus());
        subscription.setSubscriptionNextStatus(request.getSubscriptionNextStatus());
        subscription.setNextStatusChangeDate(request.getNextStatusChangeDate());
        subscription.setNextStatusChangeReason(request.getNextStatusChangeReason());
        subscription.setRootItemRefType(request.getRootItemRefType());
        subscription.setRootItemRef(request.getRootItemRef());
        subscription.setUserRefType(request.getUserRefType());
        subscription.setUserRef(request.getUserRef());
        subscription.setAlternateUserRef(request.getAlternateUserRef());
        subscription.setSubscriptionSource(request.getSubscriptionSource());
        subscription.setSubscriptionSourceRef(request.getSubscriptionSourceRef());
        subscription.setBillingFrequency(request.getBillingFrequency());
        subscription.setPeriodType(request.getPeriodType());
        subscription.setPeriodFrequency(request.getPeriodFrequency());
        subscription.setNextBillDate(request.getNextBillDate());
        subscription.setPreferredPaymentAccountId(request.getPreferredPaymentAccountId());
        subscription.setCurrency(request.getCurrency());
        subscription.setNeedGrantEntitlements(request.isNeedGrantEntitlements());
        subscription.setSubscriptionAdjustments(request.getSubscriptionAdjustments());
        return subscription;
    }

    @SuppressWarnings("unchecked")
    protected List<I> buildSubscriptionItems(
            @lombok.NonNull SubscriptionCreationRequest creationRequest,
            @Nullable ContextInfo contextInfo) {
        List<I> items = new ArrayList<>();

        for (SubscriptionItemCreationRequest request : creationRequest
                .getItemCreationRequests()) {
            I item = (I) typeFactory.get(SubscriptionItem.class);
            item.setItemRefType(request.getItemRefType());
            item.setItemRef(request.getItemRef());
            item.setItemName(request.getItemName());
            item.setParentItemRefType(request.getParentItemRefType());
            item.setParentItemRef(request.getParentItemRef());
            item.setItemUnitPrice(request.getItemUnitPrice());
            item.setQuantity(request.getQuantity());
            item.setTaxable(request.isTaxable());
            item.setTaxCategory(request.getTaxCategory());
            item.setSubscriptionItemAdjustments(request.getSubscriptionItemAdjustments());
            items.add(item);
        }

        return items;
    }
}
