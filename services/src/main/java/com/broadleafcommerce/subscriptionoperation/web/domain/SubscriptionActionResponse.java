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
package com.broadleafcommerce.subscriptionoperation.web.domain;

import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionAction;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionTypes;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * A response DTO containing the available {@link SubscriptionAction SubscriptionActions} and
 * unavailable {@link SubscriptionAction SubscriptionActions} along with the unavailable reasons.
 *
 * @author Sunny Yu
 */
@Data
public class SubscriptionActionResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The available {@link SubscriptionAction SubscriptionActions} that may be possible for this
     * subscription.
     */
    private List<SubscriptionAction> availableActions = new ArrayList<>();

    /**
     * The unavailable {@link DefaultSubscriptionActionTypes SubscriptionActionTypes} along with
     * unavailable reasons for this subscription.
     */
    private Map<String, List<String>> unavailableReasonsByActionType = new HashMap<>();
}
