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

import com.broadleafcommerce.subscriptionoperation.domain.enums.SubscriptionStatusEnum;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.money.CurrencyUnit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TODO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionCreationRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Name of this subscription
     */
    private String name;

    /**
     * Current status of this subscription
     *
     * @see com.broadleafcommerce.subscriptionoperation.domain.enums.SubscriptionStatusEnum
     */
    private String subscriptionStatus = SubscriptionStatusEnum.ACTIVE.name();

    /**
     * Next status for this subscription
     *
     * @see SubscriptionStatusEnum
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
     * Type of the item for which this subscription was provisioned
     */
    private String rootItemRefType;

    /**
     * Reference to the item id for which this subscription was provision
     */
    private String rootItemRef;

    /**
     * Reference to the user to whom this subscription belongs
     */
    @NotBlank
    private String userRef;

    /**
     * Alternate reference to the user. Useful for third-party authentication system identifiers
     */
    @NotBlank
    private String alternateUserRef;

    /**
     * This field shows from which process or user action this subscription originated
     */
    @NotBlank
    private String subscriptionSource;

    /**
     * This field shows the identifier of the process or user action this subscription originated
     */
    private String subscriptionSourceRef;

    /**
     * Frequency of billing for this subscription
     *
     * @see com.broadleafcommerce.subscription.domain.DefaultSubscriptionBillingFrequencyEnum
     */
    @NotBlank
    private String billingFrequency;

    /**
     * Next date this subscription will be billed
     */
    private Date nextBillDate;

    /**
     * References an identifier of a {@link com.broadleafcommerce.billing.job.domain.PaymentAccount}
     * that is considered preferred for this subscription. Overrides the ordering of
     * {@link com.broadleafcommerce.billing.job.domain.PaymentAccount accounts} as provided by
     * {@link com.broadleafcommerce.billing.service.provider.SavedPaymentMethodProvider}
     */
    private String preferredPaymentAccountId;

    /**
     * Currency of this subscription
     */
    @NotNull
    private CurrencyUnit currency;

    /**
     * Adjustments for this subscription. This field is used for creation from an API request and is
     * not persisted with the object
     */
    // @JsonView(RequestView.class)
    // private List<SubscriptionAdjustment> subscriptionAdjustments = new ArrayList<>();

    /**
     * Whether the system has outstanding
     * {@link com.broadleafcommerce.subscription.domain.entitlement.Entitlement entitlements} to
     * grant on this subscription
     */
    private boolean needGrantEntitlements;

    private List<SubscriptionItemCreationRequest> itemCreationRequests = new ArrayList<>();

    /**
     * Miscellaneous attributes that can be set to this request in order to inform business logic
     * for adding a {@link com.broadleafcommerce.subscriptionoperation.domain.Subscription}.
     */
    private Map<String, Object> additionalAttributes = new HashMap<>();

}
