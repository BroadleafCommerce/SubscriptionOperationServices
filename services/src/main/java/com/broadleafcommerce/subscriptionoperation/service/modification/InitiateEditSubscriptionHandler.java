/*
 * Copyright (C) 2009 - 2020 Broadleaf Commerce
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
package com.broadleafcommerce.subscriptionoperation.service.modification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.policy.trackable.TrackablePolicyUtils;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType;
import com.broadleafcommerce.subscriptionoperation.service.provider.CartOperationsProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.CreateCartRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionResponse;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Handles initiating the edit-{@link Subscription} flow.
 *
 * @author Nathan Moore (nathandmoore)
 */
@RequiredArgsConstructor
public class InitiateEditSubscriptionHandler extends AbstractModifySubscriptionHandler
        implements ModifySubscriptionHandler {

    @Getter(value = AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    @Getter(value = AccessLevel.PROTECTED)
    private final CartOperationsProvider cartOperationsProvider;

    @Getter(AccessLevel.PROTECTED)
    private final MessageSource messageSource;

    @Getter(value = AccessLevel.PROTECTED, onMethod_ = @Nullable)
    @Setter(onMethod_ = @Autowired(required = false))
    private TrackablePolicyUtils policyUtils;

    @Override
    public boolean canHandle(@lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        return DefaultSubscriptionActionType.isEdit(request.getAction().getActionType());
    }

    @Override
    protected ModifySubscriptionResponse handleInternal(
            @lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        SubscriptionWithItems subscriptionWithItems = request.getSubscription();
        // Subscription subscription = subscriptionWithItems.getSubscription();
        // List<SubscriptionItem> items = subscriptionWithItems.getSubscriptionItems();

        // todo create cart and add items
        CreateCartRequest createCartRequest = typeFactory.get(CreateCartRequest.class);
        Cart cart = cartOperationsProvider.createCart(createCartRequest, contextInfo);

        ModifySubscriptionResponse response = typeFactory.get(
                ModifySubscriptionResponse.class);
        response.setSubscription(subscriptionWithItems);
        response.setCart(cart);
        return response;
    }

    @Override
    protected String[] getRequiredPermissions(@lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        // TODO Implement this method
        return new String[] {"ALL_CUSTOMER_SUBSCRIPTION", "ALL_ACCOUNT_SUBSCRIPTION",
                "EDIT_SUBSCRIPTION"};
    }
}
