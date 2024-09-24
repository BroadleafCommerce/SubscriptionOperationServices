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

import com.broadleafcommerce.common.extension.RequestView;
import com.broadleafcommerce.common.extension.ResponseView;
import com.broadleafcommerce.data.tracking.core.ContextStateAware;
import com.broadleafcommerce.data.tracking.core.filtering.business.domain.ContextState;
import com.broadleafcommerce.data.tracking.core.filtering.domain.Tracking;
import com.broadleafcommerce.money.CurrencyConsumer;
import com.broadleafcommerce.money.util.MonetaryUtils;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserTypes;
import com.broadleafcommerce.subscriptionoperation.domain.enums.SubscriptionStatuses;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.money.CurrencySupplier;
import javax.money.CurrencyUnit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is a domain representing a user's subscription to a service or a good. The particular line
 * items of the subscription are contained in {@link SubscriptionItem} objects, while any price
 * changes would be held by {@link SubscriptionAdjustment adjustments}. Changes to a subscription's
 * status should be followed by a SubscriptionStatusAudit to track them.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonView({RequestView.class, ResponseView.class})
public class Subscription implements ContextStateAware, CurrencySupplier, CurrencyConsumer {

    @JsonAlias("subscriptionId")
    private String id;

    /**
     * Name of this subscription
     */
    private String name;

    /**
     * Current status of this subscription
     *
     * @see SubscriptionStatuses
     */
    private String subscriptionStatus;

    /**
     * Next status for this subscription
     *
     * @see SubscriptionStatuses
     */
    private String subscriptionNextStatus;

    /**
     * Date when the next status should begin
     *
     * @see #subscriptionNextStatus
     */
    private Date nextStatusChangeDate;

    /**
     * Reason for the update to the next status
     *
     * @see #subscriptionNextStatus
     */
    private String nextStatusChangeReason;

    /**
     * Date this subscription will or did continue. This is likely to be used only after a
     * subscription was paused or stopped.
     */
    private Date resumeDate;

    /**
     * Type of the item for which this subscription was provisioned
     */
    private String rootItemRefType;

    /**
     * Reference to the item id for which this subscription was provision
     */
    private String rootItemRef;

    /**
     * Type of user owning this subscription
     *
     * @see DefaultUserTypes
     */
    private String userRefType;

    /**
     * Reference to the user to whom this subscription belongs
     */
    private String userRef;

    /**
     * Alternate reference to the user. Useful for third-party authentication system identifiers
     */
    private String alternateUserRef;

    /**
     * This field shows from which process or user action this subscription originated
     */
    private String subscriptionSource;

    /**
     * This field shows the identifier of the process or user action this subscription originated
     */
    private String subscriptionSourceRef;

    /**
     * Multi-tenancy support
     */
    private String siteIdentifier;

    /**
     * Frequency of billing for this subscription
     *
     * @see DefaultSubscriptionBillingFrequencyEnum
     */
    private String billingFrequency;

    /**
     * Next date this subscription will be billed
     */
    private Date nextBillDate;

    /**
     * References an identifier of a PaymentAccount that is considered preferred for this
     * subscription.
     */
    private String preferredPaymentAccountId;

    /**
     * Currency of this subscription
     */
    private CurrencyUnit currency;

    /**
     * If this subscription was upgraded or downgraded, holds the reference to the subscription
     * replacing this one
     */
    private String nextSubscription;

    /**
     * The batch id of the last billing job run for this subscription
     */
    private String lastBillingBatchId;

    /**
     * The most current status of the last {@link com.broadleafcommerce.billing.domain.BillingEvent}
     * for this subscription
     *
     * @see PaymentStatusEnum
     */
    private String lastBillingOperationStatus;

    /**
     * Last date this that the Billing Job succeeded billing this subscription
     */
    private Date lastBillingSuccessDate;

    /**
     * Date this subscription was created. This field should not be updated or set by a request.
     */
    @JsonView(ResponseView.class)
    private Date createdDate;

    /**
     * Whether this subscription is being cancelled due to a chargeback
     */
    private boolean chargeback = false;

    /**
     * The next period this subscription will cover. This value is related to 'beginPeriod' and
     * 'endPeriod' fields of {@link SubscriptionAdjustment} and {@link SubscriptionItemAdjustment}
     */
    private Integer nextPeriod;

    /**
     * Whether the system has outstanding
     * {@link com.broadleafcommerce.subscription.domain.entitlement.Entitlement entitlements} to
     * grant on this subscription
     * <p>
     * Note that this field does not have any effects if Entitlements are disabled in
     * {@link BillingJobProperties#getEntitlements()#isEnabled()}
     */
    private boolean needGrantEntitlements = false;

    /**
     * Adjustments for this subscription. This field is used for creation from an API request and is
     * not persisted with the object
     */
    @JsonView(RequestView.class)
    private List<SubscriptionAdjustment> subscriptionAdjustments = new ArrayList<>();

    /**
     * The version of this subscription. Used for checking if the requested version of the
     * subscription is up-to-date before saving changes. This should never be manually
     * decremented/incremented.
     */
    private Integer version;

    public void setCurrencyCode(String currencyCode) {
        this.currency = currencyCode == null ? null : MonetaryUtils.getCurrency(currencyCode);
    }

    /**
     * A subset of {@link Tracking} information to expose the context state for this object.
     */
    private ContextState contextState;
}
