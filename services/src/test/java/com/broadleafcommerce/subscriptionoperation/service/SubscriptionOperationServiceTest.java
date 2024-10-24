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
package com.broadleafcommerce.subscriptionoperation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionAction;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType;
import com.broadleafcommerce.subscriptionoperation.service.exception.UnsupportedSubscriptionModificationRequestException;
import com.broadleafcommerce.subscriptionoperation.service.modification.ModifySubscriptionHandler;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionResponse;

import java.util.Collections;
import java.util.List;

import io.azam.ulidj.ULID;

/**
 * @author Nathan Moore (nathandmoore)
 */
@ExtendWith(MockitoExtension.class)
public class SubscriptionOperationServiceTest {

    @Mock
    SubscriptionProvider<SubscriptionWithItems> subscriptionProvider;

    @Mock
    SubscriptionValidationService subscriptionValidationService;

    @Mock
    ModifySubscriptionHandler testHandler;

    SubscriptionOperationService<SubscriptionWithItems> subscriptionOperationService;

    @BeforeEach
    void setup() {
        subscriptionOperationService =
                new DefaultSubscriptionOperationService<>(subscriptionProvider,
                        subscriptionValidationService,
                        new TypeFactory(Collections.emptyList()),
                        List.of(testHandler));
    }

    @Test
    void testThatModifySubscriptionFindsHandler() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        request.setSubscriptionId(ULID.random());
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(DefaultSubscriptionActionType.EDIT.name());
        request.setAction(action);

        SubscriptionWithItems swi = new SubscriptionWithItems();
        Subscription subscription = new Subscription();
        subscription.setId(request.getSubscriptionId());
        swi.setSubscription(subscription);
        SubscriptionItem item = new SubscriptionItem();
        item.setId(ULID.random());
        swi.getSubscriptionItems().add(item);

        when(subscriptionProvider.readSubscriptionById(eq(request.getSubscriptionId()),
                eq(null)))
                        .thenReturn(swi);
        when(testHandler.canHandle(eq(request), eq(null)))
                .thenReturn(true);
        ModifySubscriptionResponse expected = new ModifySubscriptionResponse();
        Cart cart = new Cart();
        cart.setId(ULID.random());
        expected.setCart(cart);
        when(testHandler.handle(eq(request), eq(null)))
                .thenReturn(expected);

        ModifySubscriptionResponse actual = subscriptionOperationService.modifySubscription(
                request,
                null);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testThatModifySubscriptionThrowsExceptionWhenNoHandlerFound() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        request.setSubscriptionId(ULID.random());
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(DefaultSubscriptionActionType.EDIT.name());
        request.setAction(action);

        SubscriptionWithItems swi = new SubscriptionWithItems();
        Subscription subscription = new Subscription();
        subscription.setId(request.getSubscriptionId());
        swi.setSubscription(subscription);
        SubscriptionItem item = new SubscriptionItem();
        item.setId(ULID.random());
        swi.getSubscriptionItems().add(item);

        when(subscriptionProvider.readSubscriptionById(eq(request.getSubscriptionId()),
                eq(null)))
                        .thenReturn(swi);
        when(testHandler.canHandle(eq(request), eq(null)))
                .thenReturn(false);

        assertThatThrownBy(() -> subscriptionOperationService.modifySubscription(request, null))
                .isInstanceOf(UnsupportedSubscriptionModificationRequestException.class)
                .extracting("request").isEqualTo(request);
    }

}
