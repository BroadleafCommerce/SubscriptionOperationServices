/*
 * Copyright (C) 2020 Broadleaf Commerce
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
package com.broadleafcommerce.subscriptionoperation.domain.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class CartItemAttributeConstants {

    @NoArgsConstructor
    public static final class Internal {
        public static final String SUBSCRIPTION_ACTION_FLOW = "subscriptionActionFlow";
        public static final String SUBSCRIPTION_PRICING_STRATEGY = "subscriptionPricingStrategy";
        public static final String IS_SEPARATE_FROM_PRIMARY_ITEM = "isSeparateFromPrimaryItem";
        public static final String EXISTING_SUBSCRIPTION_ID = "existingSubscriptionId";
        public static final String EXISTING_SUBSCRIPTION_NEXT_BILL_DATE =
                "existingSubscriptionNextBillDate";
        public static final String FULFILLMENT_WORKFLOW = "fulfillmentWorkflow";
    }

}
