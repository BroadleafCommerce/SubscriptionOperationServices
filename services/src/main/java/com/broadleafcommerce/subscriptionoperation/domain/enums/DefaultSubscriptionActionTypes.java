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
 * The default types of actions that can be taken against a {@link Subscription}.
 *
 * @author Sunny Yu
 */
public enum DefaultSubscriptionActionTypes {

    /**
     * Whether the user can edit this subscription.
     */
    EDIT,

    /**
     * Whether the user can change the auto-renewal value for this subscription.
     */
    CHANGE_AUTO_RENEWAL,

    /**
     * Whether the user can upgrade this subscription.
     */
    UPGRADE,

    /**
     * Whether the user can downgrade this subscription.
     */
    DOWNGRADE,

    /**
     * Whether the user can pause this subscription.
     */
    PAUSE,

    /**
     * Whether the user can resume this subscription.
     */
    RESUME,

    /**
     * Whether the user can suspend this subscription.
     */
    SUSPEND,

    /**
     * Whether the user can terminate this subscription.
     */
    TERMINATE,

    /**
     * Whether the user can reactivate this subscription.
     */
    REACTIVATE,

    /**
     * Whether the user can cancel this subscription.
     */
    CANCEL;

    public static boolean isEdit(String type) {
        return EDIT.name().equals(type);
    }

    public static boolean isChangeAutoRenewal(String type) {
        return CHANGE_AUTO_RENEWAL.name().equals(type);
    }

    public static boolean isUpgrade(String type) {
        return UPGRADE.name().equals(type);
    }

    public static boolean isDowngrade(String type) {
        return DOWNGRADE.name().equals(type);
    }

    public static boolean isPause(String type) {
        return PAUSE.name().equals(type);
    }

    public static boolean isResume(String type) {
        return RESUME.name().equals(type);
    }

    public static boolean isSuspend(String type) {
        return SUSPEND.name().equals(type);
    }

    public static boolean isTerminate(String type) {
        return TERMINATE.name().equals(type);
    }

    public static boolean isReactivate(String type) {
        return REACTIVATE.name().equals(type);
    }

    public static boolean isCancel(String type) {
        return CANCEL.name().equals(type);
    }
}
