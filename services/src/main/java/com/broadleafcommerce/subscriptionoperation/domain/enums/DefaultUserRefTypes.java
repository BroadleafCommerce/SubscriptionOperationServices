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

import org.apache.commons.lang3.StringUtils;

public enum DefaultUserRefTypes {

    /**
     * Describes Broadleaf's Account.
     */
    BLC_ACCOUNT,

    /**
     * Describes Broadleaf's Customer.
     */
    BLC_CUSTOMER;

    public static boolean isBroadleafAccount(String userRefType) {
        return StringUtils.equals(userRefType,
                DefaultUserRefTypes.BLC_ACCOUNT.name());
    }

    public static boolean isBroadleafCustomer(String userRefType) {
        return StringUtils.equals(userRefType,
                DefaultUserRefTypes.BLC_CUSTOMER.name());
    }
}
