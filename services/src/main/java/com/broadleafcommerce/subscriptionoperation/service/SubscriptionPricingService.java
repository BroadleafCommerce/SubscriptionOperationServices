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
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceResponse;

import java.util.List;

/**
 * Service for pricing subscriptions.
 */
public interface SubscriptionPricingService {

    /**
     * Prices the cart's subscription items, returning {@link SubscriptionPriceResponse
     * SubscriptionPriceResponses} describing how much the customer should be charged now, &
     * estimations for how much & when they'll be charged in the future as part of subscription
     * billing.
     *
     * @param cart The cart that we are pricing.
     * @param contextInfo Context information around sandbox and multitenant state.
     * @return a list of {@link SubscriptionPriceResponse SubscriptionPriceResponses}
     */
    List<SubscriptionPriceResponse> priceSubscriptions(Cart cart,
            @Nullable ContextInfo contextInfo);
}
