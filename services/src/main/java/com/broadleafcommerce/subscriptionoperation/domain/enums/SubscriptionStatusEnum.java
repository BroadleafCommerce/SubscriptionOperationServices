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

public enum SubscriptionStatusEnum {

    ACTIVE, CANCELLED, PENDING_CANCELLATION, PAUSED, ERROR;

    public static boolean isActive(String subscriptionStatus) {
        return ACTIVE.name().equals(subscriptionStatus);
    }

    public static boolean isCancelled(String subscriptionStatus) {
        return CANCELLED.name().equals(subscriptionStatus);
    }

    public static boolean isPendingCancellation(String subscriptionStatus) {
        return PENDING_CANCELLATION.name().equals(subscriptionStatus);
    }

    public static boolean isPaused(String subscriptionStatus) {
        return PAUSED.name().equals(subscriptionStatus);
    }

    public static boolean isError(String subscriptionStatus) {
        return ERROR.name().equals(subscriptionStatus);
    }

}
