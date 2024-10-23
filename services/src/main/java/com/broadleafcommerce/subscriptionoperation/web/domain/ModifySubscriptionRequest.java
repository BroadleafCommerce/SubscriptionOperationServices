/*
 * Copyright (C) 2009 - 2020 Broadleaf Commerce
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
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionAction;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * Represents a request to modify a user's subscription.
 *
 * @author Nathan Moore (nathandmoore)
 */
@Data
public class ModifySubscriptionRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The {@link Subscription#getId() id of the subscription} to modify.
     */
    private String subscriptionId;

    /**
     * The reference to the user requesting to perform the modification.
     */
    private String userRef;

    /**
     * The type of user requesting to preform the modification.
     *
     * @see DefaultUserRefTypes
     */
    private String userRefType;

    /**
     * The action to be performed on the specified {@link Subscription}.
     */
    private SubscriptionAction action;

    /**
     * TODO
     */
    private String reason;

    /**
     * TODO
     */
    private boolean immediateCancellation = false;

    /** TODO */
    private Subscription newSubscription;

    /**
     * The value to change {@link Subscription#isAutoRenewalEnabled()} to.
     */
    private boolean autoRenewalEnabled = false;

    /**
     * Holds the {@link SubscriptionWithItems} referenced by {@link #subscriptionId}. This should be
     * hydrated in the service-layer, not on the REST API request.
     */
    @JsonIgnore
    private transient SubscriptionWithItems subscription;

    /**
     * Miscellaneous attributes that can be set to this request in order to inform business logic
     */
    private Map<String, Object> additionalAttributes = new HashMap<>();

}
