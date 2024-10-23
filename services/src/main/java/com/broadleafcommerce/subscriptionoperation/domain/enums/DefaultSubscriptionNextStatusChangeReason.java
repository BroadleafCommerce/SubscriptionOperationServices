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
package com.broadleafcommerce.subscriptionoperation.domain.enums;

import com.broadleafcommerce.subscriptionoperation.domain.Subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The default reasons for the {@link Subscription#getNextStatusChangeReason()}.
 *
 * @author Sunny Yu
 */
@Getter
@RequiredArgsConstructor
public enum DefaultSubscriptionNextStatusChangeReason {

    /**
     * The {@link Subscription#isAutoRenewalEnabled() Subscription's auto renewal} is enabled.
     */
    ENABLED_AUTO_RENEWAL("subscription.next-status-reason.enabled-auto-renewal"),

    /**
     * The {@link Subscription#isAutoRenewalEnabled() Subscription's auto renewal} is disabled.
     */
    DISABLED_AUTO_RENEWAL("subscription.next-status-reason.disabled-auto-renewal");

    private final String messagePath;

    public static boolean isEnableAutoRenewal(String reason) {
        return ENABLED_AUTO_RENEWAL.name().equals(reason);
    }

    public static boolean isDisableAutoRenewal(String reason) {
        return DISABLED_AUTO_RENEWAL.name().equals(reason);
    }
}
