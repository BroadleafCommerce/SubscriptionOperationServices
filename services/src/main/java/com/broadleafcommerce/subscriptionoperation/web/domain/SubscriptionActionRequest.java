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

import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * A request DTO to see what actions are available for a {@link Subscription}.
 *
 * @see DefaultSubscriptionActionType
 * @author Sunny Yu
 */
@Data
public class SubscriptionActionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The id of the subscription to get the available actions for.
     */
    private String subscriptionId;

    /**
     * The type of the user making the request.
     *
     * @see DefaultUserRefTypes
     */
    @JsonIgnore
    private String userRefType;

    /**
     * The reference to the user making the request.
     */
    @JsonIgnore
    private String userRef;

    /**
     * Additional request attributes.
     */
    private Map<String, Object> requestAttributes = new HashMap<>();
}
