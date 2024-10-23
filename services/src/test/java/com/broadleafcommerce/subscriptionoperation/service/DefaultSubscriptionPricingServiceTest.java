/*
 * Copyright (C) 2019 Broadleaf Commerce
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

import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.SUBSCRIPTION_ACTION_FLOW;
import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.SUBSCRIPTION_PAYMENT_STRATEGY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.cart.client.domain.CartItem;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.money.util.MonetaryUtils;
import com.broadleafcommerce.order.common.domain.RecurringPriceDetail;
import com.broadleafcommerce.subscriptionoperation.domain.EstimatedFuturePayment;
import com.broadleafcommerce.subscriptionoperation.domain.PeriodDefinition;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceItemDetail;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceResponse;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPricingContext;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionItemReferenceType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPaymentStrategy;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultTermDurationType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.money.MonetaryAmount;

import io.azam.ulidj.ULID;

@ExtendWith(MockitoExtension.class)
public class DefaultSubscriptionPricingServiceTest {

    DefaultSubscriptionPricingService service;

    @BeforeEach
    public void setUp() {
        TypeFactory typeFactory = new TypeFactory(Collections.emptyList());
        service = spy(new DefaultSubscriptionPricingService(typeFactory));
    }

    @Test
    void testIdentificationOfRootItems() {
        Cart cart = buildCart(DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.POSTPAID.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name());

        List<CartItem> subscriptionRootItems = service.identifySubscriptionRootItems(cart, null)
                .collect(Collectors.toList());

        assertThat(subscriptionRootItems).hasSize(1);
        assertThat(subscriptionRootItems.get(0).getSku()).isEqualTo("121073");
    }

    @Test
    void testIdentificationOfRootItems_twoSubs() {
        Cart cart = buildCart(DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.POSTPAID.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name());
        cart = addExtraSubscriptionItem(cart);

        List<CartItem> subscriptionRootItems = service.identifySubscriptionRootItems(cart, null)
                .collect(Collectors.toList());

        assertThat(subscriptionRootItems).hasSize(2);

        List<String> rootItemSkus = subscriptionRootItems.stream()
                .map(CartItem::getSku)
                .collect(Collectors.toList());

        assertThat(rootItemSkus).containsExactlyInAnyOrder("121073", "ofc_365");
    }

    @Test
    void testBuildSubscriptionPricingContext() {
        Cart cart = buildCart(DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.POSTPAID.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name());

        CartItem subscriptionRootItem = service.identifySubscriptionRootItems(cart, null)
                .findFirst().get();

        SubscriptionPricingContext subscriptionPricingContext =
                service.buildSubscriptionPricingContext(subscriptionRootItem, null);

        assertThat(subscriptionPricingContext.getFlow())
                .isEqualTo(DefaultSubscriptionActionFlow.CREATE.name());
        assertThat(subscriptionPricingContext.getFlowSubmissionDate()).isNotNull();
        assertThat(subscriptionPricingContext.getExistingSubscriptionId()).isBlank();
        assertThat(subscriptionPricingContext.getPaymentStrategy())
                .isEqualTo(DefaultSubscriptionPaymentStrategy.POSTPAID.name());
        assertThat(subscriptionPricingContext.getPeriodType())
                .isEqualTo(DefaultSubscriptionPeriodType.MONTHLY.name());
        assertThat(subscriptionPricingContext.getPeriodFrequency()).isEqualTo(1);
        assertThat(subscriptionPricingContext.getTermDurationType()).isNotBlank();
        assertThat(subscriptionPricingContext.getTermDurationLength()).isGreaterThan(0);
    }

    @Test
    void testBuildSubscriptionPricingContext_postpaidPeriodDefinitions() {
        Cart cart = buildCart(DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.POSTPAID.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name());

        CartItem subscriptionRootItem = service.identifySubscriptionRootItems(cart, null)
                .findFirst().get();

        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                .plusMonths(1)
                .withDayOfMonth(7);
        Instant atypicalNextBillDate = ldt.toInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS);

        doAnswer(invocation -> atypicalNextBillDate)
                .when(service).determineAtypicalNextBillDate(any(), any(), any());

        SubscriptionPricingContext subscriptionPricingContext =
                service.buildSubscriptionPricingContext(subscriptionRootItem, null);

        PeriodDefinition period1 = subscriptionPricingContext.getPeriodDefinition(1);
        PeriodDefinition period2 = subscriptionPricingContext.getPeriodDefinition(2);
        PeriodDefinition period3 = subscriptionPricingContext.getPeriodDefinition(3);
        PeriodDefinition period4 = subscriptionPricingContext.getPeriodDefinition(4);

        assertThat(period1.getBillDate()).isEqualTo(atypicalNextBillDate);
        assertThat(period1.getBillDate()).isEqualTo(period1.getPeriodEndDate().plusNanos(1));
        assertThat(period1.getPeriodStartDate())
                .isEqualTo(Instant.now().truncatedTo(ChronoUnit.DAYS));
        assertThat(period1.getPeriodEndDate()).isEqualTo(atypicalNextBillDate.minusNanos(1));

        long daysBetween1and2 =
                ChronoUnit.DAYS.between(period1.getBillDate(), period2.getBillDate());
        assertThat(daysBetween1and2).isGreaterThanOrEqualTo(28);
        assertThat(daysBetween1and2).isLessThanOrEqualTo(31);
        assertThat(period2.getBillDate()).isEqualTo(period2.getPeriodEndDate().plusNanos(1));
        assertThat(period2.getPeriodStartDate()).isEqualTo(period1.getPeriodEndDate().plusNanos(1));
        assertThat(period2.getPeriodEndDate())
                .isEqualTo(period3.getPeriodStartDate().minusNanos(1));

        long daysBetween2and3 =
                ChronoUnit.DAYS.between(period2.getBillDate(), period3.getBillDate());
        assertThat(daysBetween2and3).isGreaterThanOrEqualTo(28);
        assertThat(daysBetween2and3).isLessThanOrEqualTo(31);
        assertThat(period3.getBillDate()).isEqualTo(period3.getPeriodEndDate().plusNanos(1));
        assertThat(period3.getPeriodStartDate()).isEqualTo(period2.getPeriodEndDate().plusNanos(1));
        assertThat(period3.getPeriodEndDate())
                .isEqualTo(period4.getPeriodStartDate().minusNanos(1));
    }

    @Test
    void testBuildSubscriptionPricingContext_inAdvancePeriodDefinitions() {
        Cart cart = buildCart(DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.IN_ADVANCE.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name());

        CartItem subscriptionRootItem = service.identifySubscriptionRootItems(cart, null)
                .findFirst().get();

        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                .plusMonths(1)
                .withDayOfMonth(7);
        Instant atypicalNextBillDate = ldt.toInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS);

        doAnswer(invocation -> atypicalNextBillDate)
                .when(service).determineAtypicalNextBillDate(any(), any(), any());

        SubscriptionPricingContext subscriptionPricingContext =
                service.buildSubscriptionPricingContext(subscriptionRootItem, null);

        PeriodDefinition period1 = subscriptionPricingContext.getPeriodDefinition(1);
        PeriodDefinition period2 = subscriptionPricingContext.getPeriodDefinition(2);
        PeriodDefinition period3 = subscriptionPricingContext.getPeriodDefinition(3);
        PeriodDefinition period4 = subscriptionPricingContext.getPeriodDefinition(4);

        assertThat(period1.getBillDate()).isEqualTo(atypicalNextBillDate);
        assertThat(period1.getPeriodStartDate()).isEqualTo(atypicalNextBillDate);
        assertThat(period1.getPeriodEndDate())
                .isEqualTo(period2.getPeriodStartDate().minusNanos(1));
        long daysBetweenBillAndEnd = ChronoUnit.DAYS.between(period1.getPeriodStartDate(),
                period1.getPeriodEndDate().truncatedTo(ChronoUnit.DAYS)
                        .plus(1, ChronoUnit.DAYS));
        assertThat(daysBetweenBillAndEnd).isGreaterThanOrEqualTo(28);
        assertThat(daysBetweenBillAndEnd).isLessThanOrEqualTo(31);

        long daysBetween1and2 =
                ChronoUnit.DAYS.between(period1.getBillDate(), period2.getBillDate());
        assertThat(daysBetween1and2).isGreaterThanOrEqualTo(28);
        assertThat(daysBetween1and2).isLessThanOrEqualTo(31);
        assertThat(period2.getBillDate()).isEqualTo(period2.getPeriodStartDate());
        assertThat(period2.getPeriodStartDate()).isEqualTo(period1.getPeriodEndDate().plusNanos(1));
        assertThat(period2.getPeriodEndDate())
                .isEqualTo(period3.getPeriodStartDate().minusNanos(1));

        long daysBetween2and3 =
                ChronoUnit.DAYS.between(period2.getBillDate(), period3.getBillDate());
        assertThat(daysBetween2and3).isGreaterThanOrEqualTo(28);
        assertThat(daysBetween2and3).isLessThanOrEqualTo(31);
        assertThat(period3.getBillDate()).isEqualTo(period3.getPeriodStartDate());
        assertThat(period3.getPeriodStartDate()).isEqualTo(period2.getPeriodEndDate().plusNanos(1));
        assertThat(period3.getPeriodEndDate())
                .isEqualTo(period4.getPeriodStartDate().minusNanos(1));
    }

    @Test
    void testPriceSubscriptions_dueNowZero() {
        Cart cart = buildCart(DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.POSTPAID.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name());

        CartItem subscriptionRootCartItem = cart.getCartItems().get(0);
        CartItem childCartItem = subscriptionRootCartItem.getDependentCartItems().get(0);

        List<SubscriptionPriceResponse> subscriptionPriceResponses =
                service.priceSubscriptions(cart, null);

        assertThat(subscriptionPriceResponses).hasSize(1);
        SubscriptionPriceResponse response = subscriptionPriceResponses.get(0);

        // Item references
        assertThat(response.getRootCartItemId()).isEqualTo(subscriptionRootCartItem.getId());
        assertThat(response.getRootItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(response.getRootItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());

        // Due Now details
        MonetaryAmount zeroUSD = MonetaryUtils.zero("USD");
        assertThat(response.getAmountDueNow()).isEqualTo(zeroUSD);
        assertThat(response.getProratedAmount()).isEqualTo(zeroUSD);
        assertThat(response.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(response.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(response.getDueNowItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem = response.getDueNowItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem.getItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(parentItem.getParentItemRefType()).isNull();
        assertThat(parentItem.getParentItemRef()).isNull();
        assertThat(parentItem.getAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem.getProratedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem = response.getDueNowItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem.getParentItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(childItem.getAmount()).isEqualTo(zeroUSD);
        assertThat(childItem.getProratedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem.getPriorUnbilledAmount()).isEqualTo(zeroUSD);
    }

    @Test
    void testPriceSubscriptions_dueNow_postpaid() {
        Cart cart = buildCart(true,
                DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.POSTPAID.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name(), 1);

        CartItem subscriptionRootCartItem = cart.getCartItems().get(0);
        CartItem childCartItem = subscriptionRootCartItem.getDependentCartItems().get(0);

        List<SubscriptionPriceResponse> subscriptionPriceResponses =
                service.priceSubscriptions(cart, null);

        assertThat(subscriptionPriceResponses).hasSize(1);
        SubscriptionPriceResponse response = subscriptionPriceResponses.get(0);

        // Item references
        assertThat(response.getRootCartItemId()).isEqualTo(subscriptionRootCartItem.getId());
        assertThat(response.getRootItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(response.getRootItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());

        // Due Now details
        MonetaryAmount thirtyNineUSD = MonetaryUtils.toAmount(39.00, "USD");
        MonetaryAmount twentyNineUSD = MonetaryUtils.toAmount(29.00, "USD");
        MonetaryAmount tenUSD = MonetaryUtils.toAmount(10.00, "USD");
        MonetaryAmount zeroUSD = MonetaryUtils.zero("USD");
        assertThat(response.getAmountDueNow()).isEqualTo(thirtyNineUSD);
        assertThat(response.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(response.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(response.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(response.getDueNowItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem = response.getDueNowItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem.getItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(parentItem.getParentItemRefType()).isNull();
        assertThat(parentItem.getParentItemRef()).isNull();
        assertThat(parentItem.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem = response.getDueNowItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem.getParentItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(childItem.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem.getPriorUnbilledAmount()).isEqualTo(zeroUSD);
    }

    @Test
    void testPriceSubscriptions_dueNow_inAdvance() {
        Cart cart = buildCart(true,
                DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.IN_ADVANCE.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name(), 1);

        CartItem subscriptionRootCartItem = cart.getCartItems().get(0);
        CartItem childCartItem = subscriptionRootCartItem.getDependentCartItems().get(0);

        List<SubscriptionPriceResponse> subscriptionPriceResponses =
                service.priceSubscriptions(cart, null);

        assertThat(subscriptionPriceResponses).hasSize(1);
        SubscriptionPriceResponse response = subscriptionPriceResponses.get(0);

        // Item references
        assertThat(response.getRootCartItemId()).isEqualTo(subscriptionRootCartItem.getId());
        assertThat(response.getRootItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(response.getRootItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());

        // Due Now details
        MonetaryAmount thirtyNineUSD = MonetaryUtils.toAmount(39.00, "USD");
        MonetaryAmount twentyNineUSD = MonetaryUtils.toAmount(29.00, "USD");
        MonetaryAmount tenUSD = MonetaryUtils.toAmount(10.00, "USD");
        MonetaryAmount zeroUSD = MonetaryUtils.zero("USD");
        assertThat(response.getAmountDueNow()).isEqualTo(thirtyNineUSD);
        assertThat(response.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(response.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(response.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(response.getDueNowItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem = response.getDueNowItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem.getItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(parentItem.getParentItemRefType()).isNull();
        assertThat(parentItem.getParentItemRef()).isNull();
        assertThat(parentItem.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem = response.getDueNowItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem.getParentItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(childItem.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem.getPriorUnbilledAmount()).isEqualTo(zeroUSD);
    }

    @Test
    void testPriceSubscriptions_estimatedFuturePayments_postpaid() {
        Cart cart = buildCart(false,
                DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.POSTPAID.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name(), 1);

        CartItem subscriptionRootCartItem = cart.getCartItems().get(0);
        CartItem childCartItem = subscriptionRootCartItem.getDependentCartItems().get(0);

        CartItem subscriptionRootItem = service.identifySubscriptionRootItems(cart, null)
                .findFirst().get();

        SubscriptionPricingContext subscriptionPricingContext =
                service.buildSubscriptionPricingContext(subscriptionRootItem, null);

        List<SubscriptionPriceResponse> subscriptionPriceResponses =
                service.priceSubscriptions(cart, null);

        assertThat(subscriptionPriceResponses).hasSize(1);
        SubscriptionPriceResponse response = subscriptionPriceResponses.get(0);
        List<EstimatedFuturePayment> estimatedFuturePayments =
                response.getEstimatedFuturePayments();

        assertThat(estimatedFuturePayments).hasSize(12);
        PeriodDefinition period1 = subscriptionPricingContext.getPeriodDefinition(1);
        PeriodDefinition period2 = subscriptionPricingContext.getPeriodDefinition(2);
        EstimatedFuturePayment estimatedFuturePayment1 = estimatedFuturePayments.get(0);
        EstimatedFuturePayment estimatedFuturePayment2 = estimatedFuturePayments.get(1);

        MonetaryAmount thirtyNineUSD = MonetaryUtils.toAmount(39.00, "USD");
        MonetaryAmount twentyNineUSD = MonetaryUtils.toAmount(29.00, "USD");
        MonetaryAmount tenUSD = MonetaryUtils.toAmount(10.00, "USD");
        MonetaryAmount zeroUSD = MonetaryUtils.zero("USD");

        assertThat(estimatedFuturePayment1.getBillDate()).isEqualTo(period1.getBillDate());
        assertThat(estimatedFuturePayment1.getPeriodStartDate())
                .isEqualTo(period1.getPeriodStartDate());
        assertThat(estimatedFuturePayment1.getPeriodEndDate())
                .isEqualTo(period1.getPeriodEndDate());
        assertThat(estimatedFuturePayment1.getAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment1.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(estimatedFuturePayment1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment1.getItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem1 = estimatedFuturePayment1.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem1.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem1.getItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(parentItem1.getParentItemRefType()).isNull();
        assertThat(parentItem1.getParentItemRef()).isNull();
        assertThat(parentItem1.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem1.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem1 = estimatedFuturePayment1.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem1.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem1.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem1.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem1.getParentItemRef())
                .isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(childItem1.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem1.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment2.getBillDate()).isEqualTo(period2.getBillDate());
        assertThat(estimatedFuturePayment2.getPeriodStartDate())
                .isEqualTo(period2.getPeriodStartDate());
        assertThat(estimatedFuturePayment2.getPeriodEndDate())
                .isEqualTo(period2.getPeriodEndDate());
        assertThat(estimatedFuturePayment2.getAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment2.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(estimatedFuturePayment2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment2.getItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem2 = estimatedFuturePayment2.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem2.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem2.getItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(parentItem2.getParentItemRefType()).isNull();
        assertThat(parentItem2.getParentItemRef()).isNull();
        assertThat(parentItem2.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem2.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem2 = estimatedFuturePayment2.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem2.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem2.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem2.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem2.getParentItemRef())
                .isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(childItem2.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem2.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);
    }

    @Test
    void testPriceSubscriptions_estimatedFuturePayments_inAdvance() {
        Cart cart = buildCart(true,
                DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.IN_ADVANCE.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name(), 1);

        CartItem subscriptionRootCartItem = cart.getCartItems().get(0);
        CartItem childCartItem = subscriptionRootCartItem.getDependentCartItems().get(0);

        CartItem subscriptionRootItem = service.identifySubscriptionRootItems(cart, null)
                .findFirst().get();

        SubscriptionPricingContext subscriptionPricingContext =
                service.buildSubscriptionPricingContext(subscriptionRootItem, null);

        List<SubscriptionPriceResponse> subscriptionPriceResponses =
                service.priceSubscriptions(cart, null);

        assertThat(subscriptionPriceResponses).hasSize(1);
        SubscriptionPriceResponse response = subscriptionPriceResponses.get(0);
        List<EstimatedFuturePayment> estimatedFuturePayments =
                response.getEstimatedFuturePayments();

        assertThat(estimatedFuturePayments).hasSize(12);
        PeriodDefinition period1 = subscriptionPricingContext.getPeriodDefinition(1);
        PeriodDefinition period2 = subscriptionPricingContext.getPeriodDefinition(2);
        EstimatedFuturePayment estimatedFuturePayment1 = estimatedFuturePayments.get(0);
        EstimatedFuturePayment estimatedFuturePayment2 = estimatedFuturePayments.get(1);

        MonetaryAmount thirtyNineUSD = MonetaryUtils.toAmount(39.00, "USD");
        MonetaryAmount twentyNineUSD = MonetaryUtils.toAmount(29.00, "USD");
        MonetaryAmount tenUSD = MonetaryUtils.toAmount(10.00, "USD");
        MonetaryAmount zeroUSD = MonetaryUtils.zero("USD");

        assertThat(estimatedFuturePayment1.getBillDate()).isEqualTo(period1.getBillDate());
        assertThat(estimatedFuturePayment1.getPeriodStartDate())
                .isEqualTo(period1.getPeriodStartDate());
        assertThat(estimatedFuturePayment1.getPeriodEndDate())
                .isEqualTo(period1.getPeriodEndDate());
        assertThat(estimatedFuturePayment1.getAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment1.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(estimatedFuturePayment1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment1.getItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem1 = estimatedFuturePayment1.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem1.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem1.getItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(parentItem1.getParentItemRefType()).isNull();
        assertThat(parentItem1.getParentItemRef()).isNull();
        assertThat(parentItem1.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem1.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem1 = estimatedFuturePayment1.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem1.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem1.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem1.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem1.getParentItemRef())
                .isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(childItem1.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem1.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment2.getBillDate()).isEqualTo(period2.getBillDate());
        assertThat(estimatedFuturePayment2.getPeriodStartDate())
                .isEqualTo(period2.getPeriodStartDate());
        assertThat(estimatedFuturePayment2.getPeriodEndDate())
                .isEqualTo(period2.getPeriodEndDate());
        assertThat(estimatedFuturePayment2.getAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment2.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(estimatedFuturePayment2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment2.getItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem2 = estimatedFuturePayment2.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem2.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem2.getItemRef()).isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(parentItem2.getParentItemRefType()).isNull();
        assertThat(parentItem2.getParentItemRef()).isNull();
        assertThat(parentItem2.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem2.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem2 = estimatedFuturePayment2.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem2.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem2.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem2.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem2.getParentItemRef())
                .isEqualTo(subscriptionRootCartItem.getProductId());
        assertThat(childItem2.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem2.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);
    }

    @Test
    void testPriceSubscriptions_estimatedFuturePayments_postpaid_atypicalBillDate() {
        Cart cart = buildCart(false,
                DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.POSTPAID.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name(), 1);

        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                .plusMonths(1)
                .withDayOfMonth(7);
        Instant atypicalNextBillDate = ldt.toInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS);

        doAnswer(invocation -> atypicalNextBillDate)
                .when(service).determineAtypicalNextBillDate(any(), any(), any());

        CartItem subscriptionRootItem = service.identifySubscriptionRootItems(cart, null)
                .findFirst().get();
        CartItem childCartItem = subscriptionRootItem.getDependentCartItems().get(0);

        SubscriptionPricingContext subscriptionPricingContext =
                service.buildSubscriptionPricingContext(subscriptionRootItem, null);

        List<SubscriptionPriceResponse> subscriptionPriceResponses =
                service.priceSubscriptions(cart, null);

        assertThat(subscriptionPriceResponses).hasSize(1);
        SubscriptionPriceResponse response = subscriptionPriceResponses.get(0);
        List<EstimatedFuturePayment> estimatedFuturePayments =
                response.getEstimatedFuturePayments();

        assertThat(estimatedFuturePayments).hasSize(12);
        PeriodDefinition period1 = subscriptionPricingContext.getPeriodDefinition(1);
        PeriodDefinition period2 = subscriptionPricingContext.getPeriodDefinition(2);
        EstimatedFuturePayment estimatedFuturePayment1 = estimatedFuturePayments.get(0);
        EstimatedFuturePayment estimatedFuturePayment2 = estimatedFuturePayments.get(1);

        MonetaryAmount thirtyNineUSD = MonetaryUtils.toAmount(39.00, "USD");
        MonetaryAmount twentyNineUSD = MonetaryUtils.toAmount(29.00, "USD");
        MonetaryAmount tenUSD = MonetaryUtils.toAmount(10.00, "USD");
        MonetaryAmount zeroUSD = MonetaryUtils.zero("USD");

        assertThat(estimatedFuturePayment1.getBillDate()).isEqualTo(period1.getBillDate());
        assertThat(estimatedFuturePayment1.getPeriodStartDate())
                .isEqualTo(period1.getPeriodStartDate());
        assertThat(estimatedFuturePayment1.getPeriodEndDate())
                .isEqualTo(period1.getPeriodEndDate());
        assertThat(MonetaryUtils.isLessThan(estimatedFuturePayment1.getAmount(), 39.00)).isTrue();
        assertThat(MonetaryUtils.isLessThan(estimatedFuturePayment1.getProratedAmount(), 39.00))
                .isTrue();
        assertThat(estimatedFuturePayment1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(estimatedFuturePayment1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment1.getItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem1 = estimatedFuturePayment1.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem1.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem1.getItemRef()).isEqualTo(subscriptionRootItem.getProductId());
        assertThat(parentItem1.getParentItemRefType()).isNull();
        assertThat(parentItem1.getParentItemRef()).isNull();
        assertThat(MonetaryUtils.isLessThan(parentItem1.getAmount(), 29.00)).isTrue();
        assertThat(MonetaryUtils.isLessThan(parentItem1.getProratedAmount(), 29.00)).isTrue();
        assertThat(parentItem1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem1 = estimatedFuturePayment1.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem1.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem1.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem1.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem1.getParentItemRef()).isEqualTo(subscriptionRootItem.getProductId());
        assertThat(MonetaryUtils.isLessThan(childItem1.getAmount(), 10.00)).isTrue();
        assertThat(MonetaryUtils.isLessThan(childItem1.getProratedAmount(), 10.00)).isTrue();
        assertThat(childItem1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment2.getBillDate()).isEqualTo(period2.getBillDate());
        assertThat(estimatedFuturePayment2.getPeriodStartDate())
                .isEqualTo(period2.getPeriodStartDate());
        assertThat(estimatedFuturePayment2.getPeriodEndDate())
                .isEqualTo(period2.getPeriodEndDate());
        assertThat(estimatedFuturePayment2.getAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment2.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(estimatedFuturePayment2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment2.getItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem2 = estimatedFuturePayment2.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem2.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem2.getItemRef()).isEqualTo(subscriptionRootItem.getProductId());
        assertThat(parentItem2.getParentItemRefType()).isNull();
        assertThat(parentItem2.getParentItemRef()).isNull();
        assertThat(parentItem2.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem2.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem2 = estimatedFuturePayment2.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem2.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem2.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem2.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem2.getParentItemRef()).isEqualTo(subscriptionRootItem.getProductId());
        assertThat(childItem2.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem2.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);
    }

    @Test
    void testPriceSubscriptions_estimatedFuturePayments_inAdvance_atypicalBillDate() {
        Cart cart = buildCart(false,
                DefaultSubscriptionActionFlow.CREATE.name(),
                DefaultSubscriptionPaymentStrategy.IN_ADVANCE.name(),
                DefaultSubscriptionPeriodType.MONTHLY.name(), 1);

        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                .plusMonths(1)
                .withDayOfMonth(7);
        Instant atypicalNextBillDate = ldt.toInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS);

        doAnswer(invocation -> atypicalNextBillDate)
                .when(service).determineAtypicalNextBillDate(any(), any(), any());

        CartItem subscriptionRootItem = service.identifySubscriptionRootItems(cart, null)
                .findFirst().get();
        CartItem childCartItem = subscriptionRootItem.getDependentCartItems().get(0);

        SubscriptionPricingContext subscriptionPricingContext =
                service.buildSubscriptionPricingContext(subscriptionRootItem, null);

        List<SubscriptionPriceResponse> subscriptionPriceResponses =
                service.priceSubscriptions(cart, null);

        assertThat(subscriptionPriceResponses).hasSize(1);
        SubscriptionPriceResponse response = subscriptionPriceResponses.get(0);
        List<EstimatedFuturePayment> estimatedFuturePayments =
                response.getEstimatedFuturePayments();

        assertThat(estimatedFuturePayments).hasSize(12);
        PeriodDefinition period1 = subscriptionPricingContext.getPeriodDefinition(1);
        PeriodDefinition period2 = subscriptionPricingContext.getPeriodDefinition(2);
        EstimatedFuturePayment estimatedFuturePayment1 = estimatedFuturePayments.get(0);
        EstimatedFuturePayment estimatedFuturePayment2 = estimatedFuturePayments.get(1);

        MonetaryAmount thirtyNineUSD = MonetaryUtils.toAmount(39.00, "USD");
        MonetaryAmount twentyNineUSD = MonetaryUtils.toAmount(29.00, "USD");
        MonetaryAmount tenUSD = MonetaryUtils.toAmount(10.00, "USD");
        MonetaryAmount zeroUSD = MonetaryUtils.zero("USD");

        assertThat(estimatedFuturePayment1.getBillDate()).isEqualTo(period1.getBillDate());
        assertThat(estimatedFuturePayment1.getPeriodStartDate())
                .isEqualTo(period1.getPeriodStartDate());
        assertThat(estimatedFuturePayment1.getPeriodEndDate())
                .isEqualTo(period1.getPeriodEndDate());
        assertThat(estimatedFuturePayment1.getAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment1.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(estimatedFuturePayment1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment1.getItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem1 = estimatedFuturePayment1.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem1.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem1.getItemRef()).isEqualTo(subscriptionRootItem.getProductId());
        assertThat(parentItem1.getParentItemRefType()).isNull();
        assertThat(parentItem1.getParentItemRef()).isNull();
        assertThat(parentItem1.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem1.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem1 = estimatedFuturePayment1.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem1.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem1.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem1.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem1.getParentItemRef()).isEqualTo(subscriptionRootItem.getProductId());
        assertThat(childItem1.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem1.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem1.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem1.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment2.getBillDate()).isEqualTo(period2.getBillDate());
        assertThat(estimatedFuturePayment2.getPeriodStartDate())
                .isEqualTo(period2.getPeriodStartDate());
        assertThat(estimatedFuturePayment2.getPeriodEndDate())
                .isEqualTo(period2.getPeriodEndDate());
        assertThat(estimatedFuturePayment2.getAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment2.getProratedAmount()).isEqualTo(thirtyNineUSD);
        assertThat(estimatedFuturePayment2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(estimatedFuturePayment2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        assertThat(estimatedFuturePayment2.getItemDetails()).hasSize(2);
        SubscriptionPriceItemDetail parentItem2 = estimatedFuturePayment2.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        subscriptionRootItem.getId()))
                .findFirst().orElseThrow();
        assertThat(parentItem2.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(parentItem2.getItemRef()).isEqualTo(subscriptionRootItem.getProductId());
        assertThat(parentItem2.getParentItemRefType()).isNull();
        assertThat(parentItem2.getParentItemRef()).isNull();
        assertThat(parentItem2.getAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem2.getProratedAmount()).isEqualTo(twentyNineUSD);
        assertThat(parentItem2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(parentItem2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);

        SubscriptionPriceItemDetail childItem2 = estimatedFuturePayment2.getItemDetails().stream()
                .filter(itemDetail -> Objects.equals(itemDetail.getCartItemId(),
                        childCartItem.getId()))
                .findFirst().orElseThrow();
        assertThat(childItem2.getItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem2.getItemRef()).isEqualTo(childCartItem.getProductId());
        assertThat(childItem2.getParentItemRefType())
                .isEqualTo(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        assertThat(childItem2.getParentItemRef()).isEqualTo(subscriptionRootItem.getProductId());
        assertThat(childItem2.getAmount()).isEqualTo(tenUSD);
        assertThat(childItem2.getProratedAmount()).isEqualTo(tenUSD);
        assertThat(childItem2.getCreditedAmount()).isEqualTo(zeroUSD);
        assertThat(childItem2.getPriorUnbilledAmount()).isEqualTo(zeroUSD);
    }

    private Cart buildCart(String subscriptionActionFlow,
            String subscriptionPaymentStrategy,
            String periodType) {
        return buildCart(false, subscriptionActionFlow, subscriptionPaymentStrategy, periodType, 1);
    }

    private Cart buildCart(boolean includeDueNowPrice,
            String subscriptionActionFlow,
            String subscriptionPaymentStrategy,
            String periodType,
            int periodFrequency) {
        Cart cart = new Cart();
        cart.setId(ULID.random());
        cart.setStatus("IN_PROCESS");

        CartItem parentItem = new CartItem();
        parentItem.setId(ULID.random());
        parentItem.setProductId(ULID.random());
        parentItem.setSku("121073");
        parentItem.setQuantity(1);
        parentItem.setType("STANDARD");
        parentItem.setName("1 Month 8 GB Phone Plan");
        parentItem.setUnitPriceType("standardPrice");
        if (includeDueNowPrice) {
            parentItem.setUnitPrice(MonetaryUtils.toAmount(29.00, "USD"));
            parentItem.setSubtotal(MonetaryUtils.toAmount(29.00, "USD"));
            parentItem.setAdjustmentsTotal(MonetaryUtils.zero("USD"));
            parentItem.setTotal(MonetaryUtils.toAmount(29.00, "USD"));
        } else {
            parentItem.setUnitPrice(MonetaryUtils.zero("USD"));
            parentItem.setSubtotal(MonetaryUtils.zero("USD"));
            parentItem.setAdjustmentsTotal(MonetaryUtils.zero("USD"));
            parentItem.setTotal(MonetaryUtils.zero("USD"));
        }
        parentItem.setPricingStrategy("ADD_TO_PARENT");
        parentItem.setTermDurationType(DefaultTermDurationType.MONTHS.name());
        parentItem.setTermDurationLength(1);
        parentItem.getInternalAttributes().put(SUBSCRIPTION_ACTION_FLOW, subscriptionActionFlow);
        parentItem.getInternalAttributes().put(SUBSCRIPTION_PAYMENT_STRATEGY,
                subscriptionPaymentStrategy);

        RecurringPriceDetail parentRecurringPrice = new RecurringPriceDetail();
        parentRecurringPrice.setPrice(MonetaryUtils.toAmount(29.00, "USD"));
        parentRecurringPrice.setPeriodType(periodType);
        parentRecurringPrice.setPeriodFrequency(periodFrequency);

        parentItem.setRecurringPrice(parentRecurringPrice);
        cart.getCartItems().add(parentItem);

        CartItem childItem = new CartItem();
        childItem.setId(ULID.random());
        childItem.setProductId(ULID.random());
        childItem.setSku("98765");
        childItem.setQuantity(1);
        childItem.setType("STANDARD");
        childItem.setName("Premium Entertainment Package");
        childItem.setUnitPriceType("standardPrice");
        if (includeDueNowPrice) {
            childItem.setUnitPrice(MonetaryUtils.toAmount(10.00, "USD"));
            childItem.setSubtotal(MonetaryUtils.toAmount(10.00, "USD"));
            childItem.setAdjustmentsTotal(MonetaryUtils.zero("USD"));
            childItem.setTotal(MonetaryUtils.toAmount(10.00, "USD"));
        } else {
            childItem.setUnitPrice(MonetaryUtils.zero("USD"));
            childItem.setSubtotal(MonetaryUtils.zero("USD"));
            childItem.setAdjustmentsTotal(MonetaryUtils.zero("USD"));
            childItem.setTotal(MonetaryUtils.zero("USD"));
        }
        childItem.setPricingStrategy("ADD_TO_PARENT");
        childItem.setTermDurationType(DefaultTermDurationType.MONTHS.name());
        childItem.setTermDurationLength(1);

        RecurringPriceDetail childRecurringPrice = new RecurringPriceDetail();
        childRecurringPrice.setPrice(MonetaryUtils.toAmount(10.00, "USD"));
        childRecurringPrice.setPeriodType(periodType);
        childRecurringPrice.setPeriodFrequency(periodFrequency);

        childItem.setRecurringPrice(childRecurringPrice);
        parentItem.getDependentCartItems().add(childItem);

        return cart;
    }

    private Cart addExtraSubscriptionItem(Cart cart) {
        CartItem item = new CartItem();
        item.setId(ULID.random());
        item.setProductId(ULID.random());
        item.setSku("ofc_365");
        item.setQuantity(1);
        item.setType("STANDARD");
        item.setName("Office 365");
        item.setUnitPriceType("standardPrice");
        item.setUnitPrice(MonetaryUtils.zero("USD"));
        item.setSubtotal(MonetaryUtils.zero("USD"));
        item.setAdjustmentsTotal(MonetaryUtils.zero("USD"));
        item.setTotal(MonetaryUtils.zero("USD"));
        item.setPricingStrategy("ADD_TO_PARENT");
        item.setTermDurationType(DefaultTermDurationType.MONTHS.name());
        item.setTermDurationLength(1);

        RecurringPriceDetail recurringPriceDetail = new RecurringPriceDetail();
        recurringPriceDetail.setPrice(MonetaryUtils.toAmount(100.00, "USD"));
        recurringPriceDetail.setPeriodType(DefaultSubscriptionPeriodType.MONTHLY.name());
        recurringPriceDetail.setPeriodFrequency(1);

        item.setRecurringPrice(recurringPriceDetail);
        cart.getCartItems().add(item);

        return cart;
    }

}
