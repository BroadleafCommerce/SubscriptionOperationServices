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
import java.util.ArrayList;
import java.util.List;

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

    private String id;

    /**
     * Reference the subscription for this item
     */
    private String subscriptionId;

    /**
     * Item name
     */
    private String itemName;

    /**
     * Type of item that this object represents. For example, BLC_PRODUCT.
     */
    private String itemRefType;

    /**
     * Reference to the id of the item represented by this object. For example, the product id.
     */
    private String itemRef;

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
    private int quantity = 1;

    /**
     * Whether this item is taxable
     */
    private boolean taxable = true;

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
     * Adjustments for this subscription. This field is used for creation from an API request and is
     * not persisted with the object
     */
    private List<SubscriptionItemAdjustment> subscriptionItemAdjustments = new ArrayList<>();

    /**
     * A subset of {@link Tracking} information to expose the context state for this object.
     */
    private ContextState contextState;
}
