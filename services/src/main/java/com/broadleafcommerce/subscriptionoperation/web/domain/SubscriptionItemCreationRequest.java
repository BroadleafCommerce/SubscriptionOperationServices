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

import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItemAdjustment;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionItemCreationRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Item name
     */
    private String itemName;

    /**
     * Type of item that this object represents
     */
    private String itemRefType;

    /**
     * Reference to the item ID
     */
    private String itemRef;

    /**
     * Type of the parent subscription item's backing item if this is a child subscription item
     */
    private String parentItemRefType;

    /**
     * Reference to the item underlying the parent subscription item if this is a child subscription
     * item
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
     * Adjustments for this subscription. This field is used for creation from an API request and is
     * not persisted with the object
     */
    private List<SubscriptionItemAdjustment> subscriptionItemAdjustments = new ArrayList<>();

    /**
     * Miscellaneous attributes that can be set to this request in order to inform business logic
     * for adding a {@link com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem}.
     */
    private Map<String, Object> additionalAttributes = new HashMap<>();
}
