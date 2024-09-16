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


import static com.broadleafcommerce.subscriptionoperation.provider.jpa.environment.RouteConstants.Persistence.SUBSCRIPTION_OPS_ROUTE_KEY;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.common.extension.data.DataRouteByKey;
import com.broadleafcommerce.common.messaging.notification.DetachedDurableMessageSender;
import com.broadleafcommerce.common.messaging.service.DefaultMessageLockService;
import com.broadleafcommerce.common.messaging.service.IdempotentMessageConsumptionService;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.subscriptionoperation.provider.jpa.environment.SubscriptionOperationProviderProperties;

import io.azam.ulidj.ULID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@DataRouteByKey(SUBSCRIPTION_OPS_ROUTE_KEY)
public class SubscriptionOrderCreatedListener {

    @Getter(AccessLevel.PROTECTED)
    private final IdempotentMessageConsumptionService idempotentMessageService;

    @Getter(AccessLevel.PROTECTED)
    private final SubscriptionCreatedProducer subscriptionCreatedProducer;

    @Getter(value = AccessLevel.PROTECTED)
    private final DetachedDurableMessageSender sender;

    @Getter(AccessLevel.PROTECTED)
    private final SubscriptionOperationProviderProperties providerProperties;

    @Getter(AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    @StreamListener(value = OrderCreatedConsumer.CHANNEL)
    public void listen(Message<OrderCreatedEvent> message) {
        getIdempotentMessageService().consumeMessage(message,
                SubscriptionOrderCreatedListener.class.getSimpleName(), this::processMessage);
    }

    protected void processMessage(Message<OrderCreatedEvent> message) {
        String orderNumber = message.getPayload().getOrder().getOrderNumber();
        log.warn(String.format(
                "SubscriptionOperations: Received an OrderCreatedEvent for order number %s. Emitting a blank SubscriptionCreatedEvent",
                orderNumber));

        sendSubscriptionCreatedMessage(orderNumber, message.getPayload().getContextInfo());
    }

    public void sendSubscriptionCreatedMessage(String orderNumber,
            @Nullable ContextInfo contextInfo) {
        SubscriptionCreatedEvent subscriptionCreated =
                typeFactory.get(SubscriptionCreatedEvent.class);
        subscriptionCreated.setOrderNumber(orderNumber);
        subscriptionCreated.setContextInfo(contextInfo);

        if (providerProperties != null
                && !"none".equals(providerProperties.getProvider())
                && sender != null) {
            sender.send(subscriptionCreated, SubscriptionCreatedProducer.TYPE,
                    ULID.random(), SUBSCRIPTION_OPS_ROUTE_KEY);
        } else {
            log.debug("Durable sending for subscription operation is disabled. " +
                    "This occurs if the database provider is set to 'none' or if the sender does not exist.");

            Message<SubscriptionCreatedEvent> processRequestMessage =
                    MessageBuilder.withPayload(subscriptionCreated)
                            .setHeaderIfAbsent(DefaultMessageLockService.MESSAGE_IDEMPOTENCY_KEY,
                                    ULID.random())
                            .build();

            subscriptionCreatedProducer.subscriptionCreatedOutput().send(processRequestMessage);
        }
    }
}
