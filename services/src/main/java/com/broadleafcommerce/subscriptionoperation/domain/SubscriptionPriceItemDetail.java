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

import javax.money.MonetaryAmount;

import lombok.Data;

/**
 * TODO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionPriceItemDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * TODO
     */
    private String cartItemId;

    /**
     * TODO
     */
    private String itemRefType;

    /**
     * TODO
     */
    private String itemRef;

    /**
     * TODO
     */
    private String parentItemRefType;

    /**
     * TODO
     */
    private String parentItemRef;

    /**
     * TODO
     */
    private MonetaryAmount amount;

    /**
     * The subscription price, prorated relative to the next invoice date. For a new purchase, this
     * reflects the full date range. For edits, upgrades, & downgrades, this reflects the prorated
     * price between the action & the end of the billing period.
     */
    private MonetaryAmount proratedAmount;

    /**
     * For edits, upgrades, and downgrades on a subscription that’s using an In Advance payment
     * strategy, the customer has already paid the full amount for this billing cycle at the
     * previous rate.
     * <p/>
     * For example, consider a customer who is upgrading from 10 to 20 licenses at $1 each half-way
     * through a 30-day month. The user prorated amount would be $10 for 20 licenses. This user has
     * already partially paid for 10 of the licenses, so they will be due a credit of $5 toward the
     * second-half-of-the-month amount.
     * <p/>
     * Note: This typically only impacts the first period in the estimatedFuturePayments list.
     */
    private MonetaryAmount creditedAmount;

    /**
     * Prior to an edit, upgrade, or downgrade on a subscription that’s using a Postpaid payment
     * strategy, the system will have prior unbilled charges that will be part of the next invoice.
     * In this case, the user will owe this in addition to the prorated charges for the changes
     * being made.
     * <p/>
     * For example, consider a customer who is upgrading from 10 to 20 licenses at $1 each half-way
     * through a 30-day month. The user prorated amount would be $10 for 20 licenses. This user also
     * owes $5 for access to the 10 licenses in the first half of the month.
     * <p/>
     * Note: This typically only impacts the first period in the estimatedFuturePayments list.
     */
    private MonetaryAmount priorUnbilledAmount;
}
