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

import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionTypes.CANCEL;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionTypes.DOWNGRADE;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionTypes.UPGRADE;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.policy.PolicyResponse;
import com.broadleafcommerce.data.tracking.core.policy.trackable.TrackablePolicyUtils;
import com.broadleafcommerce.subscriptionoperation.domain.Product;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionTypes;
import com.broadleafcommerce.subscriptionoperation.service.exception.InsufficientSubscriptionAccessException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionCreationRequestException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionDowngradeRequestException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionUpgradeRequestException;
import com.broadleafcommerce.subscriptionoperation.service.provider.CatalogProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionDowngradeRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionUpgradeRequest;

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
        if (CollectionUtils.isEmpty(creationRequest.getItemCreationRequests())) {
            throw new InvalidSubscriptionCreationRequestException(
                    "Subscription items must also be defined for the subscription.");
        }
    }

    @Override
    public void validateSubscriptionCancellation(
            @lombok.NonNull SubscriptionCancellationRequest cancellationRequest,
            @Nullable ContextInfo contextInfo) {
        validateUserAccessToSubscription(cancellationRequest.getSubscriptionId(), CANCEL.name(),
                contextInfo);
        // TODO Implement this method
        validateBusinessRules(cancellationRequest.getSubscriptionId(), CANCEL.name(), contextInfo);
    }

    @Override
    public void validateSubscriptionUpgrade(
            @lombok.NonNull SubscriptionUpgradeRequest upgradeRequest,
            @Nullable ContextInfo contextInfo) {
        validateUserAccessToSubscription(upgradeRequest.getPriorSubscriptionId(), UPGRADE.name(),
                contextInfo);
        // TODO Implement this method
        Product product = catalogProvider.readProductById("productId", contextInfo);
        // validate upgrade eligibility
        if (StringUtils.isBlank(product.getUpgradeProductId())) {
            throw new InvalidSubscriptionUpgradeRequestException(
                    "The subscription is not eligible for an upgrade.");
        }
        validateBusinessRules(upgradeRequest.getPriorSubscriptionId(), UPGRADE.name(), contextInfo);
    }

    @Override
    public void validateSubscriptionDowngrade(
            @lombok.NonNull SubscriptionDowngradeRequest downgradeRequest,
            @Nullable ContextInfo contextInfo) {
        validateUserAccessToSubscription(downgradeRequest.getPriorSubscriptionId(),
                DOWNGRADE.name(), contextInfo);
        // TODO Implement this method
        Product product = catalogProvider.readProductById("productId", contextInfo);
        // validate downgrade eligibility
        if (StringUtils.isBlank(product.getDowngradeProductId())) {
            throw new InvalidSubscriptionDowngradeRequestException(
                    "The subscription is not eligible for an downgrade.");
        }
        validateBusinessRules(downgradeRequest.getPriorSubscriptionId(), DOWNGRADE.name(),
                contextInfo);
    }

    // a ValidationDTO may be useful here.
    protected void validateBusinessRules(String subscriptionId,
            String actionType,
            @Nullable ContextInfo contextInfo) {
        // hook point
    }

    protected void validateUserAccessToSubscription(String subscriptionId,
            String actionType,
            @Nullable ContextInfo contextInfo) {
        // TODO Will likely need components like AuthenticationVendorPrivilegesUtility &
        // AuthenticationVendorPrivilegesSummary to validate permissions for each subscription
        if (policyUtils != null) {
            String[] permissionsRequired = getRequiredPermissions(actionType);
            PolicyResponse permResponse =
                    policyUtils.validatePermissions(permissionsRequired, contextInfo);
            if (PolicyResponse.VALID.getState() != permResponse.getState()) {
                throw new InsufficientSubscriptionAccessException(
                        "You do not have access to perform this action against this subscription.");
            }
        } else {
            log.warn(
                    "PolicyUtils is not available to validate permissions. Permission check is skipped.");
        }

        validateAdditionalPermissionRules(subscriptionId, actionType, contextInfo);
    }

    protected String[] getRequiredPermissions(String actionType) {
        // TODO Implement this method
        if (DefaultSubscriptionActionTypes.isEdit(actionType)) {
            return new String[] {"EDIT_SUBSCRIPTION"};
        } else {
            return new String[] {};
        }
    }

    protected void validateAdditionalPermissionRules(String subscriptionId,
            String actionType,
            @Nullable ContextInfo contextInfo) {
        // hookpoint
    }
}
