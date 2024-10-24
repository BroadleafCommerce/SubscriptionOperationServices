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

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.exception.EntityMissingException;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionAction;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionActionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionActionResponse;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionDowngradeRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionItemCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionUpgradeRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.jirutka.rsql.parser.ast.Node;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultSubscriptionOperationService<S extends Subscription, I extends SubscriptionItem, SWI extends SubscriptionWithItems>
        implements SubscriptionOperationService<S, I, SWI> {

    @Getter(AccessLevel.PROTECTED)
    protected final SubscriptionProvider<SWI> subscriptionProvider;

    @Getter(AccessLevel.PROTECTED)
    protected final SubscriptionValidationService subscriptionValidationService;

    @Getter(AccessLevel.PROTECTED)
    protected final TypeFactory typeFactory;

    @Override
    public SubscriptionActionResponse readSubscriptionActions(
            @NonNull SubscriptionActionRequest request,
            @Nullable ContextInfo contextInfo) {
        SWI subscription =
                subscriptionProvider.readUserSubscriptionById(request.getUserRefType(),
                        request.getUserRef(), request.getSubscriptionId(), contextInfo);

        populateSubscriptionActions(subscription, contextInfo);

        SubscriptionActionResponse response = typeFactory.get(SubscriptionActionResponse.class);
        response.setAvailableActions(subscription.getAvailableActions());
        response.setUnavailableReasonsByActionType(
                subscription.getUnavailableReasonsByActionType());
        return response;
    }

    @Override
    public Page<SWI> readSubscriptionsForUserRefTypeAndUserRef(@lombok.NonNull String userRefType,
            @lombok.NonNull String userRef,
            boolean getActions,
            @Nullable Pageable page,
            @Nullable Node filters,
            @Nullable ContextInfo contextInfo) {
        Page<SWI> subscriptions =
                subscriptionProvider.readSubscriptionsForUserRefTypeAndUserRef(userRefType, userRef,
                        page,
                        filters, contextInfo);
        if (getActions) {
            populateSubscriptionActions(subscriptions, contextInfo);
        }

        return subscriptions;
    }

    @Override
    public SWI readUserSubscriptionById(@lombok.NonNull String userRefType,
            @lombok.NonNull String userRef,
            @lombok.NonNull String subscriptionId,
            boolean getActions,
            @Nullable ContextInfo contextInfo) {
        SWI subscription =
                subscriptionProvider.readUserSubscriptionById(userRefType, userRef, subscriptionId,
                        contextInfo);
        if (getActions) {
            populateSubscriptionActions(subscription, contextInfo);
        }
        return subscription;
    }

    @Override
    public SWI readSubscriptionById(@lombok.NonNull String subscriptionId,
            @Nullable ContextInfo contextInfo) {
        return subscriptionProvider.readSubscriptionById(subscriptionId, contextInfo)
                .orElseThrow(EntityMissingException::new);
    }

    @Override
    public SWI createSubscriptionWithItems(
            @lombok.NonNull SubscriptionCreationRequest creationRequest,
            @Nullable ContextInfo contextInfo) {
        subscriptionValidationService.validateSubscriptionCreation(creationRequest, contextInfo);

        SWI subscriptionWithItemsToCreate =
                buildSubscriptionWithItems(creationRequest, contextInfo);

        return subscriptionProvider.create(subscriptionWithItemsToCreate, contextInfo);
    }

    @Override
    public S cancelSubscription(
            @lombok.NonNull SubscriptionCancellationRequest cancellationRequest,
            @Nullable ContextInfo contextInfo) {
        subscriptionValidationService.validateSubscriptionCancellation(cancellationRequest,
                contextInfo);
        return null;
    }

    @Override
    public S upgradeSubscription(@lombok.NonNull SubscriptionUpgradeRequest upgradeRequest,
            @Nullable ContextInfo contextInfo) {
        subscriptionValidationService.validateSubscriptionUpgrade(upgradeRequest, contextInfo);
        return null;
    }

    @Override
    public S downgradeSubscription(@lombok.NonNull SubscriptionDowngradeRequest downgradeRequest,
            @Nullable ContextInfo contextInfo) {
        subscriptionValidationService.validateSubscriptionDowngrade(downgradeRequest, contextInfo);
        return null;
    }

    protected void populateSubscriptionActions(Iterable<SWI> subscriptions,
            @Nullable ContextInfo contextInfo) {
        subscriptions
                .forEach(subscription -> populateSubscriptionActions(subscription, contextInfo));
    }

    protected void populateSubscriptionActions(SWI subscription,
            @Nullable ContextInfo contextInfo) {
        getAllActionTypes().forEach(action -> populateActionAvailability(subscription, action));
    }

    protected List<String> getAllActionTypes() {
        return Arrays.stream(DefaultSubscriptionActionType.values())
                .map(Enum::name)
                .toList();
    }

    protected void populateActionAvailability(SWI subscription, String actionType) {
        // TODO: Add actual logic
        if (!DefaultSubscriptionActionType.isDowngrade(actionType)) {
            subscription.getAvailableActions().add(buildAvailableAction(actionType));
        } else {
            subscription.getUnavailableReasonsByActionType().put(actionType,
                    List.of("Downgrade is not supported"));
        }
    }

    protected SubscriptionAction buildAvailableAction(String actionType) {
        SubscriptionAction action = typeFactory.get(SubscriptionAction.class);
        action.setActionType(actionType);
        return action;
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
        subscription.setAlternateUserRefType(request.getAlternateUserRefType());
        subscription.setAlternateUserRef(request.getAlternateUserRef());
        subscription.setSubscriptionSource(request.getSubscriptionSource());
        subscription.setSubscriptionSourceRef(request.getSubscriptionSourceRef());
        subscription.setSecondarySourceType(request.getSecondarySourceType());
        subscription.setSecondarySourceRef(request.getSecondarySourceRef());
        subscription.setBillingFrequency(
                StringUtils.defaultIfBlank(request.getBillingFrequency(), "USE_PERIOD_TYPE"));
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
