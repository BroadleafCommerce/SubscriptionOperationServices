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

import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPaymentStrategy;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class CartItemAttributeConstants {

    @NoArgsConstructor
    public static final class Internal {
        /**
         * Describes the subscription action flow that is being executed.
         * 
         * @see DefaultSubscriptionActionFlow
         */
        public static final String SUBSCRIPTION_ACTION_FLOW = "subscriptionActionFlow";

        /**
         * Describes whether subscription payments pay in advance of receiving goods/access vs are
         * paying for previous goods/access.
         * 
         * @see DefaultSubscriptionPaymentStrategy
         */
        public static final String SUBSCRIPTION_PAYMENT_STRATEGY = "subscriptionPaymentStrategy";

        /**
         * Describes if a given subscription item should be split into its own subscription
         */
        public static final String IS_SEPARATE_FROM_PRIMARY_ITEM = "isSeparateFromPrimaryItem";

        /**
         * The id of an existing subscription. This is most relevant for subscription action flows
         * that are modifying an existing subscription.
         */
        public static final String EXISTING_SUBSCRIPTION_ID = "existingSubscriptionId";

        /**
         * The next bill date of an existing subscription. This is most relevant for subscription
         * action flows that are modifying an existing subscription.
         */
        public static final String EXISTING_SUBSCRIPTION_NEXT_BILL_DATE =
                "existingSubscriptionNextBillDate";
    }

}
