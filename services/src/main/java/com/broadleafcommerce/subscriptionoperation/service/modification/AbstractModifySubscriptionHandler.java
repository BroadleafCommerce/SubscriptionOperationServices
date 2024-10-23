/*
 * Copyright (C) 2009 - 2020 Broadleaf Commerce
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
package com.broadleafcommerce.subscriptionoperation.service.modification;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.broadleafcommerce.common.error.validation.ValidationException;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.policy.PolicyResponse;
import com.broadleafcommerce.data.tracking.core.policy.trackable.TrackablePolicyUtils;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionAction;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.service.exception.InsufficientSubscriptionAccessException;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionResponse;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Nathan Moore (nathandmoore)
 */
@Slf4j
public abstract class AbstractModifySubscriptionHandler implements ModifySubscriptionHandler {

    @Nullable
    protected abstract TrackablePolicyUtils getPolicyUtils();

    @Override
    public ModifySubscriptionResponse handle(@lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        Assert.notNull(request.getSubscription(), "Subscription must be hydrated");
        validateRequest(request, contextInfo);
        return handleInternal(request, contextInfo);
    }

    protected abstract ModifySubscriptionResponse handleInternal(
            @lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo);

    /**
     * Validates the request to modify the subscription. It will handle throwing a
     * {@link ValidationException} if there are validation errors produced by
     * {@link #validateBusinessRules(ModifySubscriptionRequest, Errors, ContextInfo)}.
     *
     * @param request The {@link ModifySubscriptionRequest} with the details related to the
     *        modification to make.
     * @param contextInfo the current sandbox and multitenant context
     *
     * @throws InsufficientSubscriptionAccessException if the user does not have access to the
     *         subscription
     * @throws ValidationException if the request is invalid due to missing or incorrect data
     */
    protected void validateRequest(
            @lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        validateUserAccessToSubscription(request, contextInfo);

        Errors errors = new BeanPropertyBindingResult(request, "request");
        validateActionType(request, errors, contextInfo);
        validateBusinessRules(request, errors, contextInfo);

        if (errors.hasErrors()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Validates the user's access to the subscription. Called by
     * {@link #validateRequest(ModifySubscriptionRequest, ContextInfo)}.
     *
     * @param request The {@link ModifySubscriptionRequest} with the details related to the
     *        modification to make.
     * @param contextInfo the current sandbox and multitenant context
     * @throws InsufficientSubscriptionAccessException if the user does not have access to the
     *         subscription
     */
    protected void validateUserAccessToSubscription(
            @lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        // TODO Will likely need components like AuthenticationVendorPrivilegesUtility &
        // AuthenticationVendorPrivilegesSummary to validate permissions for each subscription
        if (getPolicyUtils() != null) {
            String[] permissionsRequired = getRequiredPermissions(request, contextInfo);
            PolicyResponse permResponse =
                    getPolicyUtils().validatePermissions(permissionsRequired, contextInfo);
            if (PolicyResponse.VALID.getState() != permResponse.getState()) {
                throw new InsufficientSubscriptionAccessException(
                        "You do not have access to perform this action against this subscription.");
            }
        } else {
            log.warn(
                    "PolicyUtils is not available to validate permissions. Permission check is skipped.");
        }

        validateAdditionalPermissionRules(request, contextInfo);
    }

    /**
     * Validates the {@link ModifySubscriptionRequest#getAction() requested action} against the
     * resolved {@link SubscriptionWithItems#getAvailableActions() subscription's available
     * actions}. Called by {@link #validateRequest(ModifySubscriptionRequest, ContextInfo)}.
     *
     * @param request The {@link ModifySubscriptionRequest} with the details related to the
     *        modification to make.
     * @param errors Object storing validation errors for the `request`
     * @param contextInfo the current sandbox and multitenant context
     */
    protected void validateActionType(@lombok.NonNull ModifySubscriptionRequest request,
            @lombok.NonNull Errors errors,
            @Nullable ContextInfo contextInfo) {
        List<SubscriptionAction> availableActions = request.getSubscription().getAvailableActions();
        SubscriptionAction requestedAction = request.getAction();

        if (!availableActions.contains(requestedAction)) {
            errors.rejectValue("action.actionType",
                    "subscription.modification.validation.action-type.invalid",
                    "The requested subscription cannot be cancelled.");
        }
    }

    /**
     * Hook point to supply custom business rules for specific action types. Called by
     * {@link #validateRequest(ModifySubscriptionRequest, ContextInfo)}.
     *
     * @param request The {@link ModifySubscriptionRequest} with the details related to the
     *        modification to make.
     * @param errors Object storing validation errors for the `request`
     * @param contextInfo the current sandbox and multitenant context
     */
    protected void validateBusinessRules(ModifySubscriptionRequest request,
            Errors errors,
            @Nullable ContextInfo contextInfo) {}

    /**
     * Hook point to supply the permissions required to perform the given
     * {@link SubscriptionAction}. These may be different depending on the type of user requesting
     * the modification.
     *
     * @param request The {@link ModifySubscriptionRequest} with the details related to the
     *        modification to make.
     * @param contextInfo the current sandbox and multitenant context
     * @return The required permissions for the requester to perform the modification.
     */
    protected abstract String[] getRequiredPermissions(ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo);

    /**
     * Hook point to supply additional permission validation logic. This is a no-op by default.
     *
     * @param request The {@link ModifySubscriptionRequest} with the details related to the
     *        modification to make.
     * @param contextInfo the current sandbox and multitenant context
     */
    protected void validateAdditionalPermissionRules(ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {}

}
