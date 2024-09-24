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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.broadleafcommerce.common.messaging.service.DefaultMessageLockService;
import com.broadleafcommerce.order.client.domain.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.azam.ulidj.ULID;

@SpringBootTest(properties = {
        "broadleaf.database.provider=jpa"
})
class SubscriptionOrderCreatedListenerIT {

    @Autowired
    MessageCollector collector;

    @Autowired
    SubscriptionCreatedProducer producer;

    @Autowired
    SubscriptionOrderCreatedListener listener;

    @Test
    void testOrderCreatedEventIsReceived() {
        Logger listenerLogger =
                (Logger) LoggerFactory.getLogger(SubscriptionOrderCreatedListener.class);
        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        // add the appender to the logger
        listenerLogger.addAppender(listAppender);

        // build message
        Order order = new Order();
        order.setId(ULID.random());
        order.setOrderNumber(ULID.random());
        OrderCreatedEvent event = new OrderCreatedEvent(order, new ArrayList<>(), null);
        Message<OrderCreatedEvent> message = MessageBuilder.withPayload(event)
                .setHeaderIfAbsent(DefaultMessageLockService.MESSAGE_IDEMPOTENCY_KEY,
                        ULID.random())
                .build();

        assertDoesNotThrow(() -> listener.listen(message));

        List<ILoggingEvent> loggingList = listAppender.list;
        assertThat(loggingList.get(0).getMessage()).isNotNull().contains(order.getOrderNumber());
    }

    @Test
    void testSend() {
        BlockingQueue<Message<?>> messages =
                collector.forChannel(producer.subscriptionCreatedOutput());
        messages.clear();

        listener.sendSubscriptionCreatedMessage("test", null);

        AssertionsForClassTypes.assertThat(messages.size()).isNotZero();
    }
}
