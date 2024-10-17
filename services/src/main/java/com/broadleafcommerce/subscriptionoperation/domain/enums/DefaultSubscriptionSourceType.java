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
package com.broadleafcommerce.subscriptionoperation.domain.enums;

import com.broadleafcommerce.subscriptionoperation.domain.Subscription;

/**
 * Enumerates the types of subscription sources. Used in
 * {@link Subscription#getSubscriptionSource()} and other fields.
 */
public enum DefaultSubscriptionSourceType {

    /**
     * Indicates that the subscription source for a subscription is a Broadleaf Order. Default
     * out-of-box value.
     */
    BLC_ORDER,
    /**
     * Indicates that the subscription source for a subscription is a Broadleaf Order.
     */
    BLC_ORDER_ITEM,
    /**
     * Indicates that the subscription source for a subscription is a redemption code.
     */
    REDEMPTION_CODE;

    public static boolean isBroadleafOrder(String sourceType) {
        return BLC_ORDER.name().equals(sourceType);
    }

    public static boolean isBroadleafOrderItem(String sourceType) {
        return BLC_ORDER_ITEM.name().equals(sourceType);
    }

    public static boolean isRedemptionCode(String sourceType) {
        return REDEMPTION_CODE.name().equals(sourceType);
    }
}
