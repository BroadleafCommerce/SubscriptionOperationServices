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

import org.springframework.lang.Nullable;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.cart.client.domain.CartItem;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.order.common.domain.RecurringPriceDetail;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultSubscriptionPricingService implements SubscriptionPricingService {

    @Getter(AccessLevel.PROTECTED)
    protected final TypeFactory typeFactory;

    @Override
    public List<SubscriptionPriceResponse> priceSubscriptions(@lombok.NonNull Cart cart,
            @Nullable ContextInfo contextInfo) {
        return identifySubscriptionRootItems(cart, contextInfo)
                .map(subscriptionRootItem -> priceSubscription(subscriptionRootItem, contextInfo))
                .toList();
    }

    protected Stream<CartItem> identifySubscriptionRootItems(@lombok.NonNull Cart cart,
            @Nullable ContextInfo contextInfo) {
        List<CartItem> cartItemsToConsider = cart.getCartItems();

        cart.getCartItems().stream()
                .filter(this::isSeparateFromPrimaryItem)
                .flatMap(cartItem -> cartItem.getDependentCartItems().stream())
                .forEach(cartItemsToConsider::add);

        return cartItemsToConsider.stream()
                .filter(this::hasRecurringPriceConfiguration);
    }

    protected boolean isSeparateFromPrimaryItem(@lombok.NonNull CartItem cartItem) {
        // TODO: replace with CartItem#isSeparateFromPrimaryItem, once the field has been added
        return false;
    }

    protected boolean hasRecurringPriceConfiguration(@lombok.NonNull CartItem cartItem) {
        return Optional.of(cartItem)
                .map(CartItem::getRecurringPrice)
                .map(RecurringPriceDetail::getPeriodType)
                .isPresent();
    }

    protected SubscriptionPriceResponse priceSubscription(
            @lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        SubscriptionPriceResponse response = typeFactory.get(SubscriptionPriceResponse.class);


        return response;
    }
}
