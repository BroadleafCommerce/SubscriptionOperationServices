/*
 * Copyright (C) 2019 Broadleaf Commerce
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

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.cart.client.domain.CartItem;
import com.broadleafcommerce.cart.client.domain.enums.CartItemType;
import com.broadleafcommerce.cart.client.domain.enums.DefaultCartItemTypes;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * A request DTO for adding an item to a cart.
 *
 * @author Nathan Moore (nathandmoore)
 */
@Data
public class AddItemRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The id of the variant for this add item request. Used to retrieve variant information for the
     * cart item.
     *
     * @param variantId The id of the variant for this add item request. Used to retrieve variant
     *        information for the cart item.
     * @return The id of the variant for this add item request. Used to retrieve variant information
     *         for the cart item.
     */
    private String variantId;

    /**
     * The id of the product for this add item request. Used to retrieve product information for the
     * cart item.
     *
     * @param productId The id of the product for this add item request. Used to retrieve product
     *        information for the cart item.
     * @return The id of the product for this add item request. Used to retrieve product information
     *         for the cart item.
     */
    private String productId;

    /**
     * The SKU code for the item (e.g., Product or Variant).
     *
     * @param sku SKU code for the item
     *
     * @return SKU code for the item
     */
    private String sku;

    /**
     * The {@link CartItemType type} of this item (e.g. Standard or Custom Quote Item). Defaults to
     * {@link DefaultCartItemTypes#STANDARD}.
     *
     * @param type type of this item
     * @return type of this item
     */
    private String itemType = DefaultCartItemTypes.STANDARD.name();

    /**
     * The amount of this item to be added to the {@link Cart}.
     *
     * @param quantity The amount of this item to be added to the {@link Cart}.
     * @return The amount of this item to be added to the {@link Cart}.
     */
    private int quantity;

    /**
     * Miscellaneous attributes that should be copied to the {@link CartItem#getAttributes()} for
     * this request.
     *
     * @param itemAttributes Miscellaneous attributes that should be copied to the
     *        {@link CartItem#getAttributes()} for this request.
     * @return Miscellaneous attributes that should be copied to the
     *         {@link CartItem#getAttributes()} for this request.
     */
    private Map<String, Object> itemAttributes = new HashMap<>();

    /**
     * Miscellaneous internal attributes that should be copied to the
     * {@link CartItem#getInternalAttributes()} for this request.
     *
     * @param itemAttributes Miscellaneous internal attributes that should be copied to the
     *        {@link CartItem#getInternalAttributes()} for this request.
     * @return Miscellaneous internal attributes that should be copied to the
     *         {@link CartItem#getInternalAttributes()} for this request.
     */
    private Map<String, Object> itemInternalAttributes = new HashMap<>();

    /**
     * Attribute choices that should be copied to {@link CartItem#getAttributeChoices()} for this
     * request.
     *
     * @param itemAttributeChoices Attribute choices that should be copied to
     *        {@link CartItem#getAttributeChoices()} for this request.
     * @return Attribute choices that should be copied to {@link CartItem#getAttributeChoices()} for
     *         this request.
     */
    private Map<String, String> itemAttributeChoices = new HashMap<>();

    /**
     * Attributes that should be copied to the {@link Cart#getAttributes()} for this request.
     *
     * @param cartAttributes Attributes that should be copied to the {@link Cart#getAttributes()}
     *        for this request.
     * @return Attributes that should be copied to the {@link Cart#getAttributes()} for this
     *         request.
     */
    private Map<String, Object> cartAttributes = new HashMap<>();

    /**
     * A list of additional {@link AddItemRequest AddItemRequests} that should be processed as
     * dependent items of the resulting parent item of this current {@link AddItemRequest}.
     *
     * @param dependentCartItems A list of additional {@link AddItemRequest AddItemRequests} that
     *        should be processed as dependent items of the resulting parent item of this current
     *        {@link AddItemRequest}.
     * @return A list of additional {@link AddItemRequest AddItemRequests} that should be processed
     *         as dependent items of the resulting parent item of this current
     *         {@link AddItemRequest}.
     */
    private List<AddItemRequest> dependentCartItems = new ArrayList<>();

    /**
     * If this {@link AddItemRequest} is one within {@link AddItemRequest#getDependentCartItems()},
     * this is the key to use to relate to this {@link CartItem} to a configured product item
     * choice.
     *
     * @param itemChoiceKey The key to use to relate to this {@link CartItem} to a configured
     *        product item choice.
     * @return The key to use to relate to this {@link CartItem} to a configured product item
     *         choice.
     */
    private String itemChoiceKey;

    /**
     * The value that should be mapped to {@link CartItem#getMerchandisingContext()}. This holds the
     * selector or merchandising product's id.
     *
     * @param merchandisingContext The context of the item signifying where it's added from
     *
     * @return The context of the item signifying where it's added from
     */
    private String merchandisingContext;

    /**
     * Miscellaneous attributes that can be set to this request in order to inform business logic
     * for adding a {@link CartItem}.
     *
     * @param additionalAttributes Miscellaneous attributes that can be set to this request in order
     *        to inform business logic for adding a {@link CartItem}.
     * @return Miscellaneous attributes that can be set to this request in order to inform business
     *         logic for adding a {@link CartItem}.
     */
    private Map<String, Object> additionalAttributes = new HashMap<>();

    /**
     * The type of the term duration selected for the item, if any. Some items may have terms to
     * allow certain pricing for the user. This indicates the type of duration, e.g., DAYS, WEEKS,
     * MONTHS, YEARS, for the selected term. For example, choosing between paying the full price for
     * a device (no terms) vs splitting payment over 36 months ({@link #termDurationLength} of 36
     * and {@code termDurationType} of MONTHS).
     *
     * @see #termDurationLength
     */
    private String termDurationType;

    /**
     * The length of the term duration selected for the item, if any. Some items may have terms to
     * allow certain pricing for the user. This indicates the length of duration for the selected
     * term. For example, choosing between paying the full price for a device (no terms) vs
     * splitting payment over 36 months ({@code termDurationLength} of 36 and
     * {@link #termDurationType} of MONTHS).
     *
     * @see #termDurationType
     */
    private Integer termDurationLength;

    /**
     * The version of the cart being modified. Not needed for creation requests.
     */
    private Integer version;

}
