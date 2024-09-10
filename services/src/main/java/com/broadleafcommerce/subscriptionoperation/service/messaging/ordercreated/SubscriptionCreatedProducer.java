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
package com.broadleafcommerce.subscriptionoperation.service.messaging.ordercreated;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

/**
 * Spring cloud data channel description for messaging output. This channel is primarily responsible
 * for async IO related to notifying the that a {@link SubscriptionCreatedEvent} has taken place.
 */
@SuppressWarnings("squid:S1214")
public interface SubscriptionCreatedProducer {

    String TYPE = "SUBSCRIPTION_CREATED";
    String CHANNEL = "subscriptionCreatedOutput";

    @Output(CHANNEL)
    MessageChannel subscriptionCreatedOutput();
}