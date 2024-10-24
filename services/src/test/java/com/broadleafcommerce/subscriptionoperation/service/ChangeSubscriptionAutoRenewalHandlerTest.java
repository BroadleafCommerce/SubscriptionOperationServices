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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import com.broadleafcommerce.common.error.validation.ValidationException;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionAction;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.SubscriptionStatuses;
import com.broadleafcommerce.subscriptionoperation.service.modification.ChangeSubscriptionAutoRenewalHandler;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionResponse;

import java.sql.Date;
import java.time.Instant;
import java.util.Collections;

import io.azam.ulidj.ULID;

/**
 * @author Nathan Moore (nathandmoore)
 */
@ExtendWith(MockitoExtension.class)
public class ChangeSubscriptionAutoRenewalHandlerTest {

    @Mock
    SubscriptionProvider<SubscriptionWithItems> subscriptionProvider;

    @Mock
    MessageSource messageSource;

    ChangeSubscriptionAutoRenewalHandler handler;

    @BeforeEach
    void setup() {
        handler = new ChangeSubscriptionAutoRenewalHandler(new TypeFactory(Collections.emptyList()),
                subscriptionProvider,
                messageSource);
    }

    @Test
    void testThatCanHandleReturnsTrueWhenRequestMatches() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(DefaultSubscriptionActionType.CHANGE_AUTO_RENEWAL.name());
        request.setAction(action);
        boolean actual = handler.canHandle(request, null);
        assertThat(actual).isTrue();
    }

    @Test
    void testThatCanHandleReturnsFalseWhenRequestDoesNotMatch() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(DefaultSubscriptionActionType.EDIT.name());
        request.setAction(action);
        boolean actual = handler.canHandle(request, null);
        assertThat(actual).isFalse();
    }

    @Test
    void testThatAutoRenewalIsEnabled() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        request.setAutoRenewalEnabled(true);
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(DefaultSubscriptionActionType.CHANGE_AUTO_RENEWAL.name());
        request.setAction(action);

        // represents a subscription with cancelled auto-renewal
        Subscription subscription = new Subscription();
        subscription.setId(request.getSubscriptionId());
        subscription.setAutoRenewalEnabled(false);
        subscription.setSubscriptionNextStatus(SubscriptionStatuses.CANCELLED.name());
        subscription.setNextStatusChangeDate(Date.from(Instant.now()));
        SubscriptionWithItems swi = new SubscriptionWithItems();
        swi.setSubscription(subscription);
        // just a placeholder
        SubscriptionItem item = new SubscriptionItem();
        item.setId(ULID.random());
        swi.getSubscriptionItems().add(item);
        // allow the action
        swi.getAvailableActions().add(action);

        request.setSubscription(swi);


        Subscription expectedSub = new Subscription();
        expectedSub.setId(request.getSubscriptionId());
        expectedSub.setAutoRenewalEnabled(request.isAutoRenewalEnabled());

        when(subscriptionProvider.replaceSubscription(eq(request.getSubscriptionId()),
                eq(subscription), eq(null)))
                        .thenAnswer(i -> i.getArgument(1, Subscription.class));

        ModifySubscriptionResponse actual = handler.handle(request, null);

        SubscriptionWithItems expectedSwi = new SubscriptionWithItems();
        expectedSwi.setSubscription(expectedSub);
        expectedSwi.getSubscriptionItems().add(item);
        expectedSwi.getAvailableActions().add(action);
        ModifySubscriptionResponse expected = new ModifySubscriptionResponse();
        expected.setSubscription(expectedSwi);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testThatAutoRenewalIsDisabled() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        request.setAutoRenewalEnabled(false);
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(DefaultSubscriptionActionType.CHANGE_AUTO_RENEWAL.name());
        request.setAction(action);

        // represents a subscription with auto renewal enabled
        Subscription subscription = new Subscription();
        subscription.setId(request.getSubscriptionId());
        subscription.setAutoRenewalEnabled(true);
        SubscriptionWithItems swi = new SubscriptionWithItems();
        Instant now = Instant.now();
        subscription.setEndOfTermDate(now);
        swi.setSubscription(subscription);
        // just a placeholder item
        SubscriptionItem item = new SubscriptionItem();
        item.setId(ULID.random());
        swi.getSubscriptionItems().add(item);
        // allow the action
        swi.getAvailableActions().add(action);

        request.setSubscription(swi);

        Subscription expectedSub = new Subscription();
        expectedSub.setId(request.getSubscriptionId());
        expectedSub.setAutoRenewalEnabled(request.isAutoRenewalEnabled());
        expectedSub.setSubscriptionNextStatus(SubscriptionStatuses.CANCELLED.name());
        expectedSub.setNextStatusChangeDate(Date.from(now));
        expectedSub.setEndOfTermDate(now);

        when(subscriptionProvider.replaceSubscription(eq(request.getSubscriptionId()),
                eq(subscription), eq(null)))
                        .thenAnswer(i -> i.getArgument(1, Subscription.class));

        ModifySubscriptionResponse actual = handler.handle(request, null);

        SubscriptionWithItems expectedSwi = new SubscriptionWithItems();
        expectedSwi.setSubscription(expectedSub);
        expectedSwi.getSubscriptionItems().add(item);
        expectedSwi.getAvailableActions().add(action);
        ModifySubscriptionResponse expected = new ModifySubscriptionResponse();
        expected.setSubscription(expectedSwi);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testThatAutoRenewalRequestRejectedWhenDoesNotChangeValue() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        request.setAutoRenewalEnabled(true);
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(DefaultSubscriptionActionType.CHANGE_AUTO_RENEWAL.name());
        request.setAction(action);

        // it's already enabled
        Subscription subscription = new Subscription();
        subscription.setId(request.getSubscriptionId());
        subscription.setAutoRenewalEnabled(true);
        SubscriptionWithItems swi = new SubscriptionWithItems();
        swi.setSubscription(subscription);
        SubscriptionItem item = new SubscriptionItem();
        item.setId(ULID.random());
        swi.getSubscriptionItems().add(item);
        swi.getAvailableActions().add(action);

        request.setSubscription(swi);

        assertThatThrownBy(() -> handler.handle(request, null))
                .isInstanceOf(ValidationException.class)
                .extracting("errors")
                .satisfies(extracted -> {
                    Errors errors = (Errors) extracted;
                    assertThat(errors.hasFieldErrors()).isTrue();
                    assertThat(errors.getFieldErrors())
                            .extracting(FieldError::getField, FieldError::getCode)
                            .containsExactlyInAnyOrder(tuple("autoRenewalEnabled",
                                    "subscription.modification.validation.change-auto-renewal.auto-renewal-enabled.invalid"));
                });
    }

}
