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

import org.springframework.lang.Nullable;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.subscriptionoperation.service.exception.InsufficientSubscriptionAccessException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidChangeAutoRenewalRequestException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionCreationRequestException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionDowngradeRequestException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionUpgradeRequestException;
import com.broadleafcommerce.subscriptionoperation.web.domain.ChangeAutoRenewalRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionDowngradeRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionUpgradeRequest;


/**
 * Service for validating actions against subscriptions.
 *
 * @author Sunny Yu
 */
public interface SubscriptionValidationService {

    /**
     * Validates the creation of a subscription.
     *
     * @param request the {@link SubscriptionCreationRequest}
     * @param contextInfo context information around multitenant state
     * @throws InvalidSubscriptionCreationRequestException if the request is invalid
     */
    void validateSubscriptionCreation(SubscriptionCreationRequest request,
            @Nullable ContextInfo contextInfo);

    /**
     * Validates the cancellation of a subscription.
     *
     * @param request the {@link SubscriptionCancellationRequest}
     * @param contextInfo context information around multitenant state
     * @throws InsufficientSubscriptionAccessException if the user does not have access to the
     *         subscription
     */
    void validateSubscriptionCancellation(SubscriptionCancellationRequest request,
            @Nullable ContextInfo contextInfo);

    /**
     * Validates the upgrade of a subscription.
     *
     * @param request the {@link SubscriptionUpgradeRequest}
     * @param contextInfo context information around multitenant state
     * @throws InsufficientSubscriptionAccessException if the user does not have access to the
     *         subscription
     * @throws InvalidSubscriptionUpgradeRequestException if the request is invalid
     */
    void validateSubscriptionUpgrade(SubscriptionUpgradeRequest request,
            @Nullable ContextInfo contextInfo);

    /**
     * Validates the downgrade of a subscription.
     *
     * @param request the {@link SubscriptionDowngradeRequest}
     * @param contextInfo context information around multitenant state
     * @throws InsufficientSubscriptionAccessException if the user does not have access to the
     *         subscription
     * @throws InvalidSubscriptionDowngradeRequestException if the request is invalid
     */
    void validateSubscriptionDowngrade(SubscriptionDowngradeRequest request,
            @Nullable ContextInfo contextInfo);

    /**
     * Validates the request to change the auto renewal state of a subscription.
     *
     * @param request the {@link ChangeAutoRenewalRequest}
     * @param contextInfo context information around multitenant state
     * @throws InsufficientSubscriptionAccessException if the user does not have access to the
     *         subscription
     * @throws InvalidChangeAutoRenewalRequestException if the request is invalid
     */
    void validateSubscriptionChangeAutoRenewal(ChangeAutoRenewalRequest request,
            @Nullable ContextInfo contextInfo);
}
