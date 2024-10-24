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

/**
 * Declares that payments made against a subscription are going towards the goods/services
 * rendered in the previous vs current period.
 */
public enum DefaultSubscriptionPaymentStrategy {

    /**
     * Declares that payments made against a subscription are going towards the goods or services
     * rendered in the next period.
     */
    IN_ADVANCE,

    /**
     * Declares that payments made against a subscription are going towards the goods or services
     * rendered in the previous period.
     */
    POSTPAID;

    public static boolean isPostpaid(String pricingStrategy) {
        return POSTPAID.name().equals(pricingStrategy);
    }

    public static boolean isInAdvance(String pricingStrategy) {
        return IN_ADVANCE.name().equals(pricingStrategy);
    }
}
