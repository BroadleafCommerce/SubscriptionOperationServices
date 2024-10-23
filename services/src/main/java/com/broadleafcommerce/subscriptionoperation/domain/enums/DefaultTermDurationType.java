/*
 * Copyright (C) 2021 Broadleaf Commerce
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

import com.broadleafcommerce.order.common.domain.RecurringPriceDetail;

/**
 * Defines the default term duration types possible for {@link RecurringPriceDetail recurring
 * prices}.
 *
 * @author Nathan Moore (nathandmoore)
 * @since Pricing Client 2.1.0
 *
 * @author Nathan Moore (nathandmoore)
 */
public enum DefaultTermDurationType {
    DAYS, WEEKS, MONTHS, YEARS;

    public static boolean isDays(String type) {
        return DAYS.name().equals(type);
    }

    public static boolean isWeeks(String type) {
        return WEEKS.name().equals(type);
    }

    public static boolean isMonths(String type) {
        return MONTHS.name().equals(type);
    }

    public static boolean isYears(String type) {
        return YEARS.name().equals(type);
    }
}