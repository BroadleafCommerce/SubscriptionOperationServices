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

/**
 * The default reasons on why an action is unavailable for a {@link Subscription}.
 *
 * @see DefaultSubscriptionActionType
 * @author Sunny Yu
 */
public enum DefaultSubscriptionActionUnavailableReasons {

    /**
     * The user doesn't have sufficient permissions to perform the action against this subscription.
     */
    INSUFFICIENT_PERMISSIONS,

    /**
     * There is no upgrade available for this subscription.
     */
    NO_UPGRADE_AVAILABLE,

    /**
     * There is no downgrade available for this subscription.
     */
    NO_DOWNGRADE_AVAILABLE,

    /**
     * A generic reason for something like can't change auto-renewal for subscriptions with
     * multi-year terms.
     */
    ACTION_NOT_ALLOWED,

    /**
     * The subscription is in an incorrect state for the given action.
     * <p>
     * For example, you can only {@link DefaultSubscriptionActionType#RESUME RESUME} a
     * {@link DefaultSubscriptionActionType#PAUSE PAUSED} subscription, and you can only
     * {@link DefaultSubscriptionActionType#REACTIVATE REACTIVATE} a
     * {@link DefaultSubscriptionActionType#TERMINATE TERMINATED} subscription.
     */
    INCORRECT_STATE;

    public static boolean isInsufficientPermissions(String reason) {
        return INSUFFICIENT_PERMISSIONS.name().equals(reason);
    }

    public static boolean isNoUpgradeAvailable(String reason) {
        return NO_UPGRADE_AVAILABLE.name().equals(reason);
    }

    public static boolean isNoDowngradeAvailable(String reason) {
        return NO_DOWNGRADE_AVAILABLE.name().equals(reason);
    }

    public static boolean isActionNotAllowed(String reason) {
        return ACTION_NOT_ALLOWED.name().equals(reason);
    }

    public static boolean isIncorrectState(String reason) {
        return INCORRECT_STATE.name().equals(reason);
    }
}
