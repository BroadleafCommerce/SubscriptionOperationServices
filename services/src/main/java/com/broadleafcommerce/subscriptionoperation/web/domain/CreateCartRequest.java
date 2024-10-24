/*
 * Copyright (C) 2020 Broadleaf Commerce
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
import com.broadleafcommerce.cart.client.domain.enums.DefaultCartTypes;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * A request DTO used to create a new {@link Cart}.
 *
 * @author Nathan Moore (nathandmoore)
 */
@Data
public class CreateCartRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Allows optionally setting a cart name. This is typically blank for standard, in-process
     * carts, and may be set when creating a Quote or Named cart directly.
     */
    private String name;

    /**
     * Allows optionally setting the type of cart to be created. Default is
     * {@link DefaultCartTypes#STANDARD}.
     *
     * @see DefaultCartTypes
     */
    private String type = DefaultCartTypes.STANDARD.name();

    /**
     * {@link DefaultCartTypes#QUOTE} carts can have an expiration date.
     */
    private Instant expirationDate;

    /**
     * The initial {@link AddItemRequest AddItemRequests} to populate the cart.
     */
    private List<AddItemRequest> addItemRequests = new ArrayList<>();

    /**
     * The initial {@link PriceCartRequest} to price the cart.
     */
    private PriceCartRequest priceCartRequest;

}
