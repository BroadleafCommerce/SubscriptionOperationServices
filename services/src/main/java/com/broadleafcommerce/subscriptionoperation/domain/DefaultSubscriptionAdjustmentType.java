/*
 * Copyright (C) 2009 Broadleaf Commerce
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
package com.broadleafcommerce.subscriptionoperation.domain;


/**
 * Duplicated values from com.broadleafcommerce.promotion.offer.domain.type.DiscountMethodType
 */
public enum DefaultSubscriptionAdjustmentType {
    /**
     * Take a percent off the total amount. For example, an offer using this discount method with a
     * discount value of {@code .20} being applied to an item with a price of {@code $5}, would
     * result in a new price of {@code $5 - ($5 * .20) = $4}.
     */
    PERCENT_OFF,
    /**
     * Take an amount off of the total amount. For example, an offer using this discount method with
     * a discount value of {@code 1} being applied to an item with a price of {@code $5}, would
     * result in a new price of {@code $5 - $1 = $4}.
     */
    AMOUNT_OFF,
    /**
     * Replace the amount with a fixed price. For example, an offer using this discount method with
     * a discount value of {@code 4} being applied to an item with a price of {@code $5}, would
     * result in a new price of {@code $4}.
     * <p>
     * </p>
     * <strong>Only items can have fixed price discounts—orders cannot.</strong>
     */
    FIXED_PRICE;

    public static boolean isPercentOff(String adjustmentType) {
        return PERCENT_OFF.name().equals(adjustmentType);
    }

    public static boolean isAmountOff(String adjustmentType) {
        return AMOUNT_OFF.name().equals(adjustmentType);
    }

    public static boolean isFixedPrice(String adjustmentType) {
        return FIXED_PRICE.name().equals(adjustmentType);
    }
}
