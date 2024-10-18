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
package com.broadleafcommerce.subscriptionoperation.domain;

import com.broadleafcommerce.data.tracking.core.ContextStateAware;
import com.broadleafcommerce.data.tracking.core.filtering.business.domain.ContextState;
import com.broadleafcommerce.data.tracking.core.filtering.domain.Tracking;
import com.broadleafcommerce.money.CurrencyConsumer;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.money.CurrencySupplier;
import javax.money.CurrencyUnit;

import lombok.Data;

/**
 * The product containing the details related to subscriptions.
 *
 * @author Sunny Yu
 */
@Data
public class Product
        implements Serializable, ContextStateAware, CurrencySupplier, CurrencyConsumer {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The id of the product.
     */
    private String id;

    /**
     * Name of this product.
     */
    private String name;

    /**
     * Currency for all of the prices on this product
     */
    private CurrencyUnit currency;

    /**
     * The id of the product that this product can upgrade to.
     */
    private String upgradeProductId;

    /**
     * The id of the product that this product can downgrade to.
     */
    private String downgradeProductId;

    /**
     * The number of days after which a quantity decrease is not allowed. {@code null} means no
     * restriction.
     */
    private Integer restrictDowngradeAfterDays;

    /**
     * The max number of active subscriptions that can be created for this product. {@code null}
     * means no restriction.
     */
    private Integer maxNumberOfActiveSubscriptions;

    /**
     * A subset of {@link Tracking} information to expose the context state for this object.
     *
     * @param contextState a subset of {@link Tracking} information to expose the context state for
     *        this object
     * @return a subset of {@link Tracking} information to expose the context state for this object
     */
    private ContextState contextState;

    /**
     * Map holding any additional attributes passed in the request not matching any defined
     * properties.
     */
    @JsonIgnore
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Takes in any additional attributes passed in the request not matching any defined properties.
     *
     * @param name Name of the additional attribute
     *
     * @param value Value of the additional attribute
     */
    @JsonAnySetter
    public void addAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Return any additional attributes passed in the request not matching any defined properties.
     *
     * @return any additional attributes passed in the request not matching any defined properties.
     */
    @JsonAnyGetter
    public Map<String, Object> getAttribute() {
        return attributes;
    }
}
