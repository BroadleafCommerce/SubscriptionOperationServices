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

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionChangeTierRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;


/**
 *
 */
public interface SubscriptionOperationService<S extends Subscription, I extends SubscriptionItem, SWI extends SubscriptionWithItems> {

    /**
     *
     * @param subscriptionCreationRequest
     * @param contextInfo
     * @return
     */
    SWI createSubscriptionWithItems(SubscriptionCreationRequest subscriptionCreationRequest,
            ContextInfo contextInfo);

    /**
     *
     * @param subscriptionCancellationRequest
     * @param context
     * @return
     */
    S cancelSubscription(SubscriptionCancellationRequest subscriptionCancellationRequest,
            ContextInfo context);

    /**
     *
     * @param changeTierRequest
     * @param contextInfo
     * @return
     */
    S upgradeSubscription(SubscriptionChangeTierRequest changeTierRequest, ContextInfo contextInfo);
}
