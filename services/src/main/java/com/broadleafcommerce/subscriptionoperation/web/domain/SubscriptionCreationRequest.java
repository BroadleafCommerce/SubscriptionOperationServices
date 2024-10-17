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

import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionAdjustment;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionSourceType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.broadleafcommerce.subscriptionoperation.domain.enums.SubscriptionStatuses;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.money.CurrencyUnit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A request DTO that stores fields necessary for creating a {@link Subscription} and its
 * {@link SubscriptionItem items}. {@link SubscriptionAdjustment Adjustments} may also be created
 * with this request
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
     * @see SubscriptionStatuses
     */
    private String subscriptionStatus = SubscriptionStatuses.ACTIVE.name();

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
     * Type of user owning this subscription
     *
     * @see DefaultUserRefTypes
     */
    private String userRefType;

    /**
     * Reference to the user to whom this subscription belongs
     */
    private String userRef;

    /**
     * Type of the alternative user reference
     *
     * @see DefaultUserRefTypes
     */
    private String alternateUserRefType;

    /**
     * Alternate reference to the user. Useful for third-party authentication system identifiers
     */
    private String alternateUserRef;

    /**
     * This field shows from which process or user action this subscription originated.
     *
     * @see #subscriptionSourceRef
     * @see DefaultSubscriptionSourceType
     */
    private String subscriptionSource;

    /**
     * This field shows the identifier of the process or user action this subscription originated.
     *
     * @see #subscriptionSource
     */
    private String subscriptionSourceRef;

    /**
     * The type of the secondary source to reference, e.g, an Order Item on an Order. Optional.
     *
     * @see #secondarySourceRef
     * @see DefaultSubscriptionSourceType
     */
    private String secondarySourceType;

    /**
     * The secondary source to reference, e.g, an Order Item on an Order. Optional.
     *
     * @see #secondarySourceType
     */
    private String secondarySourceRef;

    /**
     * Frequency of billing for this subscription
     *
     * @deprecated in favor of {@link #periodType} & {@link #periodFrequency}
     */
    @Deprecated
    private String billingFrequency;

    /**
     * The frequency with which the recurring price should be charged., e.g., a value of 1 combined
     * with {@link #periodType} of MONTH would indicate to a subscription service that the price
     * should be charged every 1 month.
     *
     * @see #periodType
     */
    private Integer periodFrequency = 1;

    /**
     * The period type for the price, e.g. MONTHLY, QUARTERLY, ANNUALLY
     *
     * @see #periodFrequency
     */
    private String periodType;

    /**
     * Next date this subscription will be billed
     */
    private Date nextBillDate;

    /**
     * References an identifier of a BillingServices PaymentAccount that is considered preferred for
     * this subscription.
     */
    private String preferredPaymentAccountId;

    /**
     * Type of the item for which this subscription was provisioned
     */
    private String rootItemRefType;

    /**
     * Reference to the item id for which this subscription was provision
     */
    private String rootItemRef;

    private List<SubscriptionItemCreationRequest> itemCreationRequests = new ArrayList<>();

    /**
     * Adjustments for this subscription. This field is used for creation from an API request and is
     * not persisted with the object
     */
    private List<SubscriptionAdjustment> subscriptionAdjustments = new ArrayList<>();

    /**
     * Currency of this subscription
     */
    private CurrencyUnit currency;

    /**
     * Whether the system has outstanding entitlements to grant for this subscription
     */
    private boolean needGrantEntitlements = false;

    /**
     * Miscellaneous attributes that can be set to this request in order to inform business logic
     * for adding a {@link com.broadleafcommerce.subscriptionoperation.domain.Subscription}.
     */
    private Map<String, Object> additionalAttributes = new HashMap<>();

}
