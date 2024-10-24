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

import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.money.CurrencyUnit;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Context object that's used throughout the subscription pricing ecosystem to communicate details
 * about the subscription, action, & subscription periods.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionPricingContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Describes the subscription action flow that is being executed.
     * 
     * @see DefaultSubscriptionActionFlow
     */
    private String flow;

    /**
     * The id of an existing subscription. This is most relevant for subscription action flows that
     * are modifying an existing subscription.
     */
    private String existingSubscriptionId;

    /**
     * Describes whether subscription payments pay in advance of receiving goods/access vs are
     * paying for previous goods/access.
     */
    private String paymentStrategy;

    /**
     * Describes the next bill date for scenarios where it's not a typical next bill date (i.e. not
     * 1 period away from the current date). For creation flows, this may describe something like
     * the first billing date after a free trial. For other flows that act upon a subscription in
     * the middle of a period, this describes the existing subscription's next bill date.
     */
    private Instant atypicalNextBillDate;

    /**
     * The frequency with which the recurring price should be charged., e.g., a value of 1 combined
     * with {@link #periodType} of MONTH would indicate to a subscription service that the price
     * should be charged every 1 month.
     *
     * @see #periodType
     */
    private int periodFrequency = 1;

    /**
     * The period type for the price, e.g. MONTHLY, QUARTERLY, ANNUALLY
     *
     * @see #periodFrequency
     */
    private String periodType;

    /**
     * The type of time interval (seconds, minutes, hours, etc.)
     */
    private String termDurationType;

    /**
     * Time interval (number of seconds, minutes, hours, etc.)
     */
    private int termDurationLength;

    /**
     * Describes upcoming subscription billing periods, including the start date, end date, & when
     * the customer will be billed. Map keys are upcoming period numbers, with 1 being the first
     * period. For creation flows, the first period represents the first time that subscription
     * billing will be engaged (i.e. the first bill following the initial purchase). For other flows
     * that act upon the subscription in the middle of a period... TODO: fill this out after working
     * edit flow!
     */
    private Map<Integer, PeriodDefinition> periodDefinitions = new HashMap<>();

    /**
     * Currency of this subscription
     */
    @NotNull
    private CurrencyUnit currency;

    /**
     * Miscellaneous attributes that can be added to the context in order to provide more
     * information.
     */
    private Map<String, Object> additionalAttributes = new HashMap<>();

    public PeriodDefinition getPeriodDefinition(Integer period) {
        return getPeriodDefinitions().get(period);
    }
}
