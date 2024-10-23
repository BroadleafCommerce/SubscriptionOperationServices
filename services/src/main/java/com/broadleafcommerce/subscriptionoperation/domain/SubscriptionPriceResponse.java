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

import com.broadleafcommerce.cart.client.domain.CartItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.money.MonetaryAmount;

import lombok.Data;

/**
 * For a subscription cart item, this object describes the amount that the customer should be
 * charged now vs their future payments.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionPriceResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Reference to the root subscription {@link CartItem}
     */
    private String rootCartItemId;

    /**
     * Type of item that this object represents. For example, BLC_PRODUCT.
     */
    private String rootItemRefType;

    /**
     * Reference to the id of the item represented by this object. For example, the product id.
     */
    private String rootItemRef;

    /**
     * Amount due today, which is typically captured immediately. Equal to
     * {@link #getProratedAmount()} + {@link #getPriorUnbilledAmount()} -
     * {@link #getCreditedAmount()}.
     * <p/>
     * Note: This value is allowed to be negative, representing a required refund.
     */
    private MonetaryAmount amountDueNow;

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

    /**
     * A list of {@link SubscriptionPriceItemDetail SubscriptionPriceItemDetails} describing the
     * individual item prices that contributed to the overall {@link #getAmountDueNow()}.
     */
    private List<SubscriptionPriceItemDetail> dueNowItemDetails = new ArrayList<>();

    /**
     * A collection of {@link EstimatedFuturePayment} objects describing the timing & amount of
     * future subscription payments.
     */
    private List<EstimatedFuturePayment> estimatedFuturePayments = new ArrayList<>();

     /**
     * TODO
     */
     // private List<RemovedAdjustment> removedAdjustments = new ArrayList<>();
}
