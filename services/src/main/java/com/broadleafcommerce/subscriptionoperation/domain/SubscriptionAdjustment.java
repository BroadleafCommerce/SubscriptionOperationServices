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


import com.broadleafcommerce.data.tracking.core.ContextStateAware;
import com.broadleafcommerce.data.tracking.core.filtering.business.domain.ContextState;
import com.broadleafcommerce.data.tracking.core.filtering.domain.Tracking;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionAdjustmentType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is a representation of a price adjustment for a {@link Subscription}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionAdjustment implements ContextStateAware {

    @JsonAlias("adjustmentId")
    private String id;

    /**
     * Reference to the {@link Subscription} whose price this adjusts
     */
    private String subscriptionId;

    /**
     * Reference to the promotion or offer that drives this adjustment
     */
    private String adjustmentRef;

    /**
     * Type of this adjustment
     *
     * @see DefaultSubscriptionAdjustmentType
     */
    private String subscriptionAdjustmentType;

    /**
     * Amount of this adjustment
     */
    private BigDecimal adjustmentAmount;

    /**
     * Number of the billing period this adjustment starts being active
     */
    private Integer beginPeriod;

    /**
     * Number of the billing period this adjustment stops. If this field is null, the adjustment is
     * considered indefinite
     */
    private Integer endPeriod;

    /**
     * A subset of {@link Tracking} information to expose the context state for this object.
     */
    private ContextState contextState;
}
