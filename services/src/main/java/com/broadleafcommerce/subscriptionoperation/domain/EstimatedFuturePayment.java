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
package com.broadleafcommerce.subscriptionoperation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.money.MonetaryAmount;

import lombok.Data;

/**
 * TODO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EstimatedFuturePayment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * TODO
     */
    private Instant billDate;

    /**
     * TODO
     */
    private Instant periodStartDate;

    /**
     * TODO
     */
    private Instant periodEndDate;

    /**
     * Amount due on the payment schedule date. Equal to {@link #getProratedAmount()} +
     * {@link #getPriorUnbilledAmount()} - {@link #getCreditedAmount()}.
     */
    private MonetaryAmount amount;

    /**
     * Prorated amount for this item (for new, this would match the amount)
     */
    private MonetaryAmount proratedAmount;

    /**
     * For edits, upgrades, and downgrades, the system has partial credits to the prorated amount
     * that the customer already paid. Typically only impacts the first period in the
     * estimatedFuturePaymentsList. For example, consider a customer who is upgrading from 10 to 20
     * licenses at $1 each half-way through a 30 day month. The user prorated amount would be $10
     * for 20 licenses. This user has already partially paid for 10 of the licenses, so they will be
     * due a credit of $5 toward the amount.
     */
    private MonetaryAmount creditedAmount;

    /**
     * Prior to an upgrade, downgrade, or edit, the system may have prior unbilled charges that will
     * be part of the next invoice. In this case, the user will owe this in addition to the prorated
     * charges for the changes being made. Typically only impacts the first period in the
     * estimatedFuturePaymentsList.
     */
    private MonetaryAmount priorUnbilledAmount;

    /**
     * TODO
     */
    private List<SubscriptionPriceItemDetail> itemDetails = new ArrayList<>();
}
