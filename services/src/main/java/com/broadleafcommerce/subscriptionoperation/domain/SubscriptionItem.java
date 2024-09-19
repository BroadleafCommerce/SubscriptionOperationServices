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
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This represents a line item for a {@link Subscription}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionItem implements ContextStateAware {

    @Size(max = 255)
    private String id;

    /**
     * Reference the subscription for this item
     */
    @Size(max = 255)
    @NotBlank
    private String subscriptionId;

    /**
     * Type of item that this object represents
     */
    private String itemRefType;

    /**
     * Reference to the id of the item represented by this object
     */
    private String itemRef;

    /**
     * Item name
     */
    private String itemName;

    /**
     * Type of the parent subscription item's backing item if this is a child subscription item
     */
    private String parentItemRefType;

    /**
     * Reference of the parent subscription item's backing item if this is a child subscription item
     */
    private String parentItemRef;

    /**
     * Unit price of the item (price of a single unit)
     */
    private BigDecimal itemUnitPrice;

    /**
     * Quantity purchased
     */
    private Integer quantity;

    /**
     * Whether this item is taxable
     */
    private Boolean taxable;

    /**
     * Tax category of the item
     */
    private String taxCategory;

    /**
     * Comma-separated nexus codes
     */
    private String taxNexus;

    /**
     * Whether this item is archived
     */
    private boolean archived = false;

    /**
     * Why this item was archived
     */
    private String archiveReason;

    /**
     * A subset of {@link Tracking} information to expose the context state for this object.
     */
    private ContextState contextState;
}
