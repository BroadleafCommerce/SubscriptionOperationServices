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

import org.springframework.lang.Nullable;

import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.Optional;

import lombok.Data;

/**
 * A custom DTO that is intended for the specific purpose of supporting PATCH for
 * {@link Subscription}.
 * <p>
 * <b>It is important to note that the combined usage of {@code @Nullable} and {@link Optional} for
 * the fields is entirely intentional.</b>. Since there is no way to distinguish between a value
 * <i>not being supplied</i> and <i>being updated to {@code null}</i>, this mechanism is necessary.
 * <p>
 * If the request JSON <i>does not</i> have a particular field's key, the field in this DTO will
 * deserialize to {@code null}. If the request JSON <i>does</i> have the field's key but the value
 * is null, the field in this DTO will deserialize to {@link Optional#empty()}.
 * <p>
 * An earlier, alternate implementation relied on a single {@code Map<String, Object>}
 * "updateFields" property to achieve the same distinction between unsupplied and null, but this
 * approach had the flaw of not being able to take advantage of Jackson's automatic type-safe
 * deserialization. With the current approach, Jackson can understand the declared types and
 * automatically verify and convert the fields to the appropriate types.
 * <p>
 * <b>Another important note is that the use of {@code @Data} and {@code @JsonInclude} with
 * {@code JsonInclude.Include.NON_NULL} is intentional.</b> This is required for making sure that
 * null fields do not get serialized as part of the request body.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionUpdateDTO {

    @Nullable
    private Optional<String> name;

    @Nullable
    private Optional<String> subscriptionStatus;

    @Nullable
    private Optional<String> subscriptionNextStatus;

    @Nullable
    private Optional<Date> nextStatusChangeDate;

    @Nullable
    private Optional<String> nextStatusChangeReason;

    @Nullable
    private Optional<Date> resumeDate;

    @Nullable
    private Optional<String> billingFrequency;

    @Nullable
    private Optional<Integer> periodFrequency;

    @Nullable
    private Optional<String> periodType;

    @Nullable
    private Optional<Date> nextBillDate;

    @Nullable
    private Optional<String> nextSubscription;

    @Nullable
    private Optional<Boolean> chargeback;

    @Nullable
    private Optional<Boolean> autoRenewalEnabled;
}
