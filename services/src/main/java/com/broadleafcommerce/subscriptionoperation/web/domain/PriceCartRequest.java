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

import com.broadleafcommerce.money.CurrencyConsumer;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

import javax.money.CurrencySupplier;
import javax.money.CurrencyUnit;

import lombok.Data;

/**
 * A DTO used to provide context on a request to price a cart.
 *
 * @author Nathan Moore (nathandmoore)
 */
@Data
public class PriceCartRequest implements Serializable, CurrencySupplier, CurrencyConsumer {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Declares if catalog prices should also be gathered as part of the repricing process.
     *
     * @param updateCatalogPricing Whether catalog prices should also be gathered as part of the
     *        repricing process.
     * @return Whether catalog prices should also be gathered as part of the repricing process.
     */
    private boolean updateCatalogPricing = false;

    /**
     * The locale to price the cart against.
     *
     * @param locale The locale to price the cart against.
     * @return The locale to price the cart against.
     */
    private Locale locale;

    /**
     * The currency to price the cart against.
     *
     * @param currency The currency to price the cart against.
     * @return The currency to price the cart against.
     */
    private CurrencyUnit currency;
}
