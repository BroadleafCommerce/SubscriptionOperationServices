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
package com.broadleafcommerce.subscriptionoperation.service.modification;

import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionNextStatusChangeReason.DISABLED_AUTO_RENEWAL;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionNextStatusChangeReason.ENABLED_AUTO_RENEWAL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.policy.trackable.TrackablePolicyUtils;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.SubscriptionStatuses;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionResponse;

import java.sql.Date;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Handles change whether auto-renewal is enabled for a {@link Subscription}.
 *
 * @author Nathan Moore (nathandmoore)
 */
@RequiredArgsConstructor
public class ChangeSubscriptionAutoRenewalHandler extends AbstractModifySubscriptionHandler
        implements ModifySubscriptionHandler {

    @Getter(value = AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    @Getter(value = AccessLevel.PROTECTED)
    private final SubscriptionProvider<SubscriptionWithItems> subscriptionProvider;

    @Getter(AccessLevel.PROTECTED)
    private final MessageSource messageSource;

    @Getter(value = AccessLevel.PROTECTED, onMethod_ = @Nullable)
    @Setter(onMethod_ = @Autowired(required = false))
    private TrackablePolicyUtils policyUtils;

    @Override
    public boolean canHandle(@lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        return DefaultSubscriptionActionType
                .isChangeAutoRenewal(request.getAction().getActionType());
    }

    @Override
    protected ModifySubscriptionResponse handleInternal(
            @lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        SubscriptionWithItems subscriptionWithItems = request.getSubscription();
        Subscription subscription = subscriptionWithItems.getSubscription();
        subscription.setAutoRenewalEnabled(request.isAutoRenewalEnabled());

        if (request.isAutoRenewalEnabled()) {
            subscription.setSubscriptionNextStatus(null);
            subscription.setNextStatusChangeDate(null);

            String reason = getMessageSource().getMessage(ENABLED_AUTO_RENEWAL.getMessagePath(),
                    null, LocaleContextHolder.getLocale());
            subscription.setNextStatusChangeReason(reason);
        } else {
            subscription.setSubscriptionNextStatus(SubscriptionStatuses.CANCELLED.name());
            subscription.setNextStatusChangeDate(Date.from(subscription.getEndOfTermDate()));

            String reason = getMessageSource().getMessage(DISABLED_AUTO_RENEWAL.getMessagePath(),
                    null, LocaleContextHolder.getLocale());
            subscription.setNextStatusChangeReason(reason);
        }

        Subscription updatedSubscription =
                subscriptionProvider.replaceSubscription(subscription.getId(),
                        subscription,
                        contextInfo);

        // todo: refresh the actions too?
        ModifySubscriptionResponse response = typeFactory.get(
                ModifySubscriptionResponse.class);
        subscriptionWithItems.setSubscription(updatedSubscription);
        response.setSubscription(subscriptionWithItems);
        return response;
    }

    @Override
    protected void validateBusinessRules(@lombok.NonNull ModifySubscriptionRequest request,
            @lombok.NonNull Errors errors,
            @Nullable ContextInfo contextInfo) {
        boolean currentAutoRenewalStatus = request.getSubscription()
                .getSubscription()
                .isAutoRenewalEnabled();
        boolean nextAutoRenewalStatus = request.isAutoRenewalEnabled();

        if (currentAutoRenewalStatus == nextAutoRenewalStatus) {
            errors.rejectValue("autoRenewalEnabled",
                    "subscription.modification.validation.change-auto-renewal.auto-renewal-enabled.invalid",
                    "Auto Renewal is already enabled.");
        }
    }

    @Override
    protected String[] getRequiredPermissions(@lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        // TODO Implement this method
        return new String[] {"ALL_CUSTOMER_SUBSCRIPTION", "ALL_ACCOUNT_SUBSCRIPTION",
                "CHANGE_SUBSCRIPTION_AUTO_RENEWAL"};
    }
}
