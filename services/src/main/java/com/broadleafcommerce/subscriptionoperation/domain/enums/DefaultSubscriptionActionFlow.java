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

/**
 * TODO
 */
public enum DefaultSubscriptionActionFlow {

    /**
     * TODO
     */
    CREATE,

    /**
     * TODO
     */
    EDIT,

    /**
     * TODO
     */
    UPGRADE,

    /**
     * TODO
     */
    DOWNGRADE,

    /**
     * TODO
     */
    TERMINATE;

    public static boolean isCreate(String flow) {
        return CREATE.name().equals(flow);
    }

    public static boolean isEdit(String flow) {
        return EDIT.name().equals(flow);
    }

    public static boolean isUpgrade(String flow) {
        return UPGRADE.name().equals(flow);
    }

    public static boolean isDowngrade(String flow) {
        return DOWNGRADE.name().equals(flow);
    }

    public static boolean isTerminate(String flow) {
        return TERMINATE.name().equals(flow);
    }
}