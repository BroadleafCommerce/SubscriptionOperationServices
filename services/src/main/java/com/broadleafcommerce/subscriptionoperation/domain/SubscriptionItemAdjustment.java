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
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This represents a line item price adjustment for a
 * {@link com.broadleafcommerce.subscription.domain.Subscription}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionItemAdjustment implements ContextStateAware {

    /**
     * @deprecated Use {@link #getId()} field instead
     */
    @Deprecated(since = "1.0.0")
    @Size(max = 255)
    private String itemAdjustmentId;

    private String id;

    /**
     * Reference to the {@link com.broadleafcommerce.subscription.domain.SubscriptionItem} whose
     * price this adjusts
     */
    @NotBlank
    private String subscriptionItemId;

    /**
     * Reference to the promotion or offer that drives this adjustment
     */
    @NotBlank
    private String adjustmentRef;

    /**
     * Type of adjustment that this adds
     *
     * @see DefaultSubscriptionAdjustmentType
     */
    private String subscriptionAdjustmentType;

    /**
     * Amount of the price adjustment
     */
    @NotNull
    private BigDecimal adjustmentAmount;

    /**
     * Number of the billing period this adjustment starts being active
     */
    @PositiveOrZero
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

    public void setItemAdjustmentId(String itemAdjustmentId) {
        this.itemAdjustmentId = itemAdjustmentId;
        this.id = itemAdjustmentId;
    }

    public void setId(String id) {
        this.itemAdjustmentId = id;
        this.id = id;
    }
}
