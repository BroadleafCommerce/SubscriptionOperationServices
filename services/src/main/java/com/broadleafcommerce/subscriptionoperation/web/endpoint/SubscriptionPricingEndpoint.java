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
package com.broadleafcommerce.subscriptionoperation.web.endpoint;

import org.broadleafcommerce.frameworkmapping.annotation.FrameworkPostMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkRestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.context.ContextOperation;
import com.broadleafcommerce.data.tracking.core.type.OperationType;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceResponse;
import com.broadleafcommerce.subscriptionoperation.service.SubscriptionPricingService;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@FrameworkRestController
public class SubscriptionPricingEndpoint {

    @Getter(AccessLevel.PROTECTED)
    protected final SubscriptionPricingService subscriptionPricingService;

    @FrameworkPostMapping(value = "/price-subscriptions")
    // TODO
    // @Policy(permissionRoots = {"SUBSCRIPTION_PRICING"})
    public List<SubscriptionPriceResponse> priceSubscriptions(
            @RequestBody Cart cart,
            @ContextOperation(OperationType.UPDATE) final ContextInfo contextInfo) {
        return subscriptionPricingService.priceSubscriptions(cart, contextInfo);
    }
}