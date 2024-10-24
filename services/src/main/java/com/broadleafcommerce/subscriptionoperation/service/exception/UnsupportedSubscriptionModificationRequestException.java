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
package com.broadleafcommerce.subscriptionoperation.service.exception;

import org.springframework.lang.NonNull;

import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.service.modification.ModifySubscriptionHandler;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;

import lombok.Getter;

/**
 * Thrown when a request is made to modify a {@link Subscription} that has no matching
 * {@link ModifySubscriptionHandler}.
 *
 * @author Nathan Moore (nathandmoore)
 */
@Getter
public class UnsupportedSubscriptionModificationRequestException extends RuntimeException {

    private final ModifySubscriptionRequest request;

    public UnsupportedSubscriptionModificationRequestException(
            @NonNull @lombok.NonNull ModifySubscriptionRequest request) {
        super("Requested action %s cannot be handled."
                .formatted(request.getAction().getActionType()));
        this.request = request;
    }

}
