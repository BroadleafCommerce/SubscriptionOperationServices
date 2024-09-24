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

public enum DefaultUserTypes {

    /**
     * Represents a customer user who is an account member.
     */
    BLC_ACCOUNT_MEMBER,

    /**
     * Represents a customer user who has permission to approve account carts.
     */
    BLC_ACCOUNT_APPROVER,

    /**
     * Represents a customer.
     */
    BLC_CUSTOMER,

    /**
     * Represents a sales representative.
     * <p>
     * Typically used for users who can manage and respond to quotes.
     */
    BLC_SALES_REP,

    /**
     * Represents the system.
     * <p>
     * Typically used for system originated changes such as a scheduled job to mark quotes as
     * expired.
     */
    BLC_SYSTEM;

    public static boolean isBroadleafAccountMember(String cartActionAuditUserType) {
        return StringUtils.equals(cartActionAuditUserType, BLC_ACCOUNT_MEMBER.name());
    }

    public static boolean isBroadleafAccountApprover(String cartActionAuditUserType) {
        return StringUtils.equals(cartActionAuditUserType, BLC_ACCOUNT_APPROVER.name());
    }

    public static boolean isBroadleafCustomer(String cartActionAuditUserType) {
        return StringUtils.equals(cartActionAuditUserType, BLC_CUSTOMER.name());
    }

    public static boolean isBroadleafSalesRep(String cartActionAuditUserType) {
        return StringUtils.equals(cartActionAuditUserType, BLC_SALES_REP.name());
    }

    public static boolean isBroadleafSystem(String cartActionAuditUserType) {
        return StringUtils.equals(cartActionAuditUserType, BLC_SYSTEM.name());
    }
}
