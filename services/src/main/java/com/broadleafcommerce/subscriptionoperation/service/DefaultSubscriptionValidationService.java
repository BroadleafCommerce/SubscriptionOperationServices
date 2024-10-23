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
package com.broadleafcommerce.subscriptionoperation.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.policy.trackable.TrackablePolicyUtils;
import com.broadleafcommerce.subscriptionoperation.domain.Product;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionCreationRequestException;
import com.broadleafcommerce.subscriptionoperation.service.provider.CatalogProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultSubscriptionValidationService implements SubscriptionValidationService {

    @Getter(AccessLevel.PROTECTED)
    private final CatalogProvider<Product> catalogProvider;

    @Getter(value = AccessLevel.PROTECTED, onMethod_ = @Nullable)
    @Setter(onMethod_ = @Autowired(required = false))
    private TrackablePolicyUtils policyUtils;

    @Getter(AccessLevel.PROTECTED)
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private SubscriptionOperationService<SubscriptionWithItems> subscriptionOperationService;

    @Override
    public void validateSubscriptionCreation(
            @lombok.NonNull SubscriptionCreationRequest creationRequest,
            @Nullable ContextInfo contextInfo) {
        if (StringUtils.isBlank(creationRequest.getUserRefType())
                || StringUtils.isBlank(creationRequest.getUserRef())) {
            throw new InvalidSubscriptionCreationRequestException(
                    "A subscription must be given an owning user/account via userRefType and userRef.");
        }
        if (StringUtils.isBlank(creationRequest.getPeriodType())
                && StringUtils.isBlank(creationRequest.getBillingFrequency())) {
            throw new InvalidSubscriptionCreationRequestException(
                    "A subscription must be given a periodType or billingFrequency.");
        }
        if (StringUtils.isBlank(creationRequest.getSubscriptionSource())
                && StringUtils.isBlank(creationRequest.getSubscriptionSourceRef())) {
            throw new InvalidSubscriptionCreationRequestException(
                    "A subscription must be given a source or sourceRef.");
        }
        if (CollectionUtils.isEmpty(creationRequest.getItemCreationRequests())) {
            throw new InvalidSubscriptionCreationRequestException(
                    "Subscription items must also be defined for the subscription.");
        }
    }

}
