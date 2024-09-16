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
package com.broadleafcommerce.subscriptionoperation.service.messaging.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.broadleafcommerce.common.extension.ConditionalOnPropertyOrGroup;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.common.messaging.notification.DetachedDurableMessageSender;
import com.broadleafcommerce.common.messaging.service.IdempotentMessageConsumptionService;
import com.broadleafcommerce.subscriptionoperation.provider.jpa.environment.SubscriptionOperationProviderProperties;
import com.broadleafcommerce.subscriptionoperation.service.messaging.ordercreated.OrderCreatedConsumer;
import com.broadleafcommerce.subscriptionoperation.service.messaging.ordercreated.SubscriptionCreatedProducer;
import com.broadleafcommerce.subscriptionoperation.service.messaging.ordercreated.SubscriptionOrderCreatedListener;

@Configuration
@ConditionalOnPropertyOrGroup(
        name = "broadleaf.subscriptionoperation.messaging.active",
        group = "broadleaf.basic.messaging.enabled",
        matchIfMissing = true)
@EnableBinding({SubscriptionCreatedProducer.class,
        OrderCreatedConsumer.class})
public class SubscriptionOperationMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SubscriptionOrderCreatedListener subscriptionOrderCreatedListener(
            IdempotentMessageConsumptionService idempotentConsumptionService,
            SubscriptionCreatedProducer subscriptionCreatedProducer,
            DetachedDurableMessageSender sender,
            SubscriptionOperationProviderProperties providerProperties,
            TypeFactory typeFactory) {
        return new SubscriptionOrderCreatedListener(
                idempotentConsumptionService,
                subscriptionCreatedProducer,
                sender,
                providerProperties,
                typeFactory);
    }
}
