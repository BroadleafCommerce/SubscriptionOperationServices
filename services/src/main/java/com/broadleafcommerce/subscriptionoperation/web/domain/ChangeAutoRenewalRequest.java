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
package com.broadleafcommerce.subscriptionoperation.web.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * A request DTO used to change the auto renewal setting of a subscription.
 *
 * @author Sunny Yu
 */
@Data
public class ChangeAutoRenewalRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The id of the subscription to change the auto renewal setting for
     */
    @NotNull
    private String subscriptionId;

    /**
     * The state of the auto renewal setting to change to
     */
    private boolean autoRenewalEnabled = true;

    /**
     * Additional attributes to be used in the request
     */
    private Map<String, Object> additionalAttributes = new HashMap<>();
}
