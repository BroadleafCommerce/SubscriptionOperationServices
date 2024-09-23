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

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionChangeTierRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionItemCreationRequest;

import java.util.ArrayList;
import java.util.List;

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
    public SWI createSubscriptionWithItems(SubscriptionCreationRequest subscriptionCreationRequest,
            ContextInfo contextInfo) {
        SWI subscriptionWithItemsToCreate = buildSubscriptionWithItems(subscriptionCreationRequest);
        return subscriptionProvider.create(subscriptionWithItemsToCreate, contextInfo);
    }

    @Override
    public S cancelSubscription(SubscriptionCancellationRequest subscriptionCancellationRequest,
            ContextInfo context) {
        return null;
    }

    @Override
    public S upgradeSubscription(SubscriptionChangeTierRequest changeTierRequest,
            ContextInfo contextInfo) {
        return null;
    }


    @SuppressWarnings("unchecked")
    protected SWI buildSubscriptionWithItems(
            SubscriptionCreationRequest subscriptionCreationRequest) {
        S subscription = buildSubscription(subscriptionCreationRequest);
        List<I> items = buildSubscriptionItems(subscriptionCreationRequest);
        SWI subscriptionWithItemsToBeCreated = (SWI) typeFactory.get(SubscriptionWithItems.class);
        subscriptionWithItemsToBeCreated.setSubscription(subscription);
        subscriptionWithItemsToBeCreated.setSubscriptionItems((List<SubscriptionItem>) items);
        return subscriptionWithItemsToBeCreated;
    }

    @SuppressWarnings("unchecked")
    protected S buildSubscription(SubscriptionCreationRequest request) {
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
        subscription.setNextBillDate(request.getNextBillDate());
        subscription.setPreferredPaymentAccountId(request.getPreferredPaymentAccountId());
        subscription.setCurrency(request.getCurrency());
        subscription.setNeedGrantEntitlements(request.isNeedGrantEntitlements());
        subscription.setSubscriptionAdjustments(request.getSubscriptionAdjustments());
        return subscription;
    }

    @SuppressWarnings("unchecked")
    protected List<I> buildSubscriptionItems(
            SubscriptionCreationRequest subscriptionCreationRequest) {
        List<I> items = new ArrayList<>();
        for (SubscriptionItemCreationRequest request : subscriptionCreationRequest
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
