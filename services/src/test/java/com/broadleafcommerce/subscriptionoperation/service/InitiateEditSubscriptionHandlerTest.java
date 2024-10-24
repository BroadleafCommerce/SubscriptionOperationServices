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

import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType.CANCEL;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType.CHANGE_AUTO_RENEWAL;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType.DOWNGRADE;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType.EDIT;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType.UPGRADE;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionItemReferenceType.BLC_PRODUCT;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.MONTHLY;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes.BLC_CUSTOMER;
import static com.broadleafcommerce.subscriptionoperation.service.modification.InitiateEditSubscriptionHandler.FLOW;
import static com.broadleafcommerce.subscriptionoperation.service.modification.InitiateEditSubscriptionHandler.SUBSCRIPTION_ID;
import static com.broadleafcommerce.subscriptionoperation.service.modification.InitiateEditSubscriptionHandler.SUBSCRIPTION_ITEM_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.money.util.MonetaryUtils;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionAction;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow;
import com.broadleafcommerce.subscriptionoperation.service.modification.InitiateEditSubscriptionHandler;
import com.broadleafcommerce.subscriptionoperation.service.provider.CartOperationProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.AddItemRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.CreateCartRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.PriceCartRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Stream;

/**
 * @author Nathan Moore (nathandmoore)
 */
@ExtendWith(MockitoExtension.class)
public class InitiateEditSubscriptionHandlerTest {

    InitiateEditSubscriptionHandler handler;

    @Mock
    CartOperationProvider<Cart> cartOperationProvider;

    @Mock
    MessageSource messageSource;

    @BeforeEach
    void setup() {
        handler = new InitiateEditSubscriptionHandler(new TypeFactory(Collections.emptyList()),
                cartOperationProvider,
                messageSource);
    }

    @Test
    void testThatCanHandleReturnsTrueWhenRequestMatches() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(EDIT.name());
        request.setAction(action);
        boolean actual = handler.canHandle(request, null);
        assertThat(actual).isTrue();
    }

    @Test
    void testThatCanHandleReturnsFalseWhenRequestDoesNotMatch() {
        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        SubscriptionAction action = new SubscriptionAction();
        action.setActionType(CHANGE_AUTO_RENEWAL.name());
        request.setAction(action);
        boolean actual = handler.canHandle(request, null);
        assertThat(actual).isFalse();
    }

    @Test
    void testThatCreateCartRequestBuiltCorrectly() {
        SubscriptionWithItems subscriptionWithItems = buildSubscriptionWithItems();

        ModifySubscriptionRequest request = new ModifySubscriptionRequest();
        request.setSubscription(subscriptionWithItems);
        request.setSubscriptionId(subscriptionWithItems.getSubscription().getId());
        SubscriptionAction edit = new SubscriptionAction();
        edit.setActionType(EDIT.name());
        request.setAction(edit);


        when(cartOperationProvider.createCart(any(CreateCartRequest.class), eq(null)))
                .thenReturn(new Cart());

        handler.handle(request, null);
        ArgumentCaptor<CreateCartRequest> createCartRequestCaptor =
                ArgumentCaptor.forClass(CreateCartRequest.class);
        verify(cartOperationProvider, Mockito.times(1))
                .createCart(createCartRequestCaptor.capture(), eq(null));

        CreateCartRequest expected = buildCreateCartRequest();
        CreateCartRequest actual = createCartRequestCaptor.getValue();
        String actualName = actual.getName();
        actual.setName(null);
        assertThat(actual).isEqualTo(expected);
        assertThat(actualName)
                .matches("Edit 12 Months Office 365 Subscription with Teams - \\w{26}");
    }

    private SubscriptionWithItems buildSubscriptionWithItems() {
        Subscription subscription = new Subscription();
        subscription.setId("subscription");
        subscription.setName("12 Months Office 365 Subscription with Teams");
        subscription.setEndOfTermDate(Instant.now().plus(365, ChronoUnit.DAYS));
        subscription.setAutoRenewalEnabled(false);
        subscription.setNextStatusChangeDate(Date.from(subscription.getEndOfTermDate()));
        subscription.setNextStatusChangeReason("Auto-renewal cancelled");
        subscription.setVersion(1);
        subscription.setTermDurationLength(12);
        subscription.setTermDurationType("MONTHS");
        subscription.setNextBillDate(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)));
        subscription.setNextPeriod(1);
        subscription.setPeriodFrequency(1);
        subscription.setPeriodType(MONTHLY.name());
        subscription.setUserRef("customer1");
        subscription.setUserRefType(BLC_CUSTOMER.name());
        subscription.setCurrency(MonetaryUtils.getCurrency("USD"));
        subscription.setRootItemRef("office365");
        subscription.setRootItemRefType(BLC_PRODUCT.name());

        SubscriptionItem office365 = new SubscriptionItem();
        office365.setId("office365SI");
        office365.setSubscriptionId(subscription.getId());
        office365.setItemName("12 Months Office 365 Subscription");
        office365.setItemRef(subscription.getRootItemRef());
        office365.setItemRefType(subscription.getRootItemRefType());
        office365.setQuantity(1);
        office365.setItemUnitPrice(BigDecimal.TEN);

        SubscriptionItem teams = getAddOnSI(subscription, office365);

        SubscriptionWithItems subscriptionWithItems = new SubscriptionWithItems();
        subscriptionWithItems.setSubscription(subscription);
        subscriptionWithItems.getSubscriptionItems().add(office365);
        subscriptionWithItems.getSubscriptionItems().add(teams);

        Stream.of(EDIT,
                UPGRADE,
                DOWNGRADE,
                CHANGE_AUTO_RENEWAL,
                CANCEL)
                .forEach(actionType -> {
                    SubscriptionAction action = new SubscriptionAction();
                    action.setActionType(actionType.name());
                    subscriptionWithItems.getAvailableActions().add(action);
                });

        return subscriptionWithItems;
    }

    private SubscriptionItem getAddOnSI(Subscription subscription,
            SubscriptionItem office365) {
        SubscriptionItem teams = new SubscriptionItem();
        teams.setId("teamsSI");
        teams.setSubscriptionId(subscription.getId());
        teams.setItemName("Teams Subscription");
        teams.setItemRef("teams");
        teams.setItemRefType(BLC_PRODUCT.name());
        teams.setQuantity(1);
        teams.setItemUnitPrice(BigDecimal.ONE);
        teams.setParentItemRef(office365.getItemRef());
        teams.setParentItemRefType(office365.getItemRefType());
        teams.setAddOnKey("addOn1");
        return teams;
    }

    private CreateCartRequest buildCreateCartRequest() {
        PriceCartRequest priceCartRequest = new PriceCartRequest();
        priceCartRequest.setCurrency(MonetaryUtils.getCurrency("USD"));
        CreateCartRequest request = new CreateCartRequest();
        request.setPriceCartRequest(priceCartRequest);
        AddItemRequest rootItem = new AddItemRequest();
        rootItem.setTermDurationLength(12);
        rootItem.setTermDurationType("MONTHS");
        rootItem.setQuantity(1);
        rootItem.setProductId("office365");
        rootItem.getItemAttributes().put(SUBSCRIPTION_ITEM_ID, "office365SI");
        rootItem.getCartAttributes().put(FLOW, DefaultSubscriptionActionFlow.EDIT.name());
        rootItem.getCartAttributes().put(SUBSCRIPTION_ID, "subscription");
        request.getAddItemRequests().add(rootItem);
        AddItemRequest addOn = new AddItemRequest();
        addOn.setTermDurationLength(12);
        addOn.setTermDurationType("MONTHS");
        addOn.setQuantity(1);
        addOn.setProductId("teams");
        addOn.setItemChoiceKey("addOn1");
        addOn.getItemAttributes().put(SUBSCRIPTION_ITEM_ID, "teamsSI");
        rootItem.getDependentCartItems().add(addOn);

        return request;
    }
}
