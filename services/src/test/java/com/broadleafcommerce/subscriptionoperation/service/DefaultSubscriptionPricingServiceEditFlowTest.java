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

import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.EXISTING_SUBSCRIPTION_ID;
import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.SUBSCRIPTION_ACTION_FLOW;
import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.SUBSCRIPTION_PAYMENT_STRATEGY;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isAnnually;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isMonthly;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isQuarterly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.cart.client.domain.CartItem;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.money.util.MonetaryUtils;
import com.broadleafcommerce.order.common.domain.RecurringPriceDetail;
import com.broadleafcommerce.subscriptionoperation.domain.PeriodDefinition;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPricingContext;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionItemReferenceType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPaymentStrategy;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultTermDurationType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.broadleafcommerce.subscriptionoperation.domain.enums.SubscriptionStatuses;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import io.azam.ulidj.ULID;

@ExtendWith(MockitoExtension.class)
public class DefaultSubscriptionPricingServiceEditFlowTest {

    DefaultSubscriptionPricingService service;

    SubscriptionProvider<SubscriptionWithItems> subscriptionProvider;

    @BeforeEach
    public void setUp() {
        TypeFactory typeFactory = new TypeFactory(Collections.emptyList());
        subscriptionProvider = mock(SubscriptionProvider.class);
        service = spy(new DefaultSubscriptionPricingService(subscriptionProvider, typeFactory));
    }

    @Test
    void testBuildSubscriptionPricingContext() {
        String subscriptionPaymentStrategy = DefaultSubscriptionPaymentStrategy.POSTPAID.name();
        String periodType = DefaultSubscriptionPeriodType.MONTHLY.name();

        Optional<SubscriptionWithItems> subscriptionWithItems = buildSubscriptionWithItems(subscriptionPaymentStrategy, periodType);

        Cart cart = buildCart(DefaultSubscriptionActionFlow.EDIT.name(),
                subscriptionWithItems.get().getSubscriptionId(),
                subscriptionPaymentStrategy, periodType);

        when(subscriptionProvider.readSubscriptionById(any(), any()))
                .thenReturn(subscriptionWithItems);

        CartItem subscriptionRootItem = service.identifySubscriptionRootItems(cart, null)
                .findFirst().get();

        SubscriptionPricingContext subscriptionPricingContext =
                service.buildSubscriptionPricingContext(subscriptionRootItem, null);

        assertThat(subscriptionPricingContext.getFlow())
                .isEqualTo(DefaultSubscriptionActionFlow.EDIT.name());
        assertThat(subscriptionPricingContext.getExistingSubscriptionId()).isNotBlank();
        assertThat(subscriptionPricingContext.getPaymentStrategy())
                .isEqualTo(subscriptionPaymentStrategy);
        assertThat(subscriptionPricingContext.getPeriodType())
                .isEqualTo(periodType);
        assertThat(subscriptionPricingContext.getPeriodFrequency()).isEqualTo(1);
        assertThat(subscriptionPricingContext.getEndOfTermsDate()).isNotNull();
    }

    @Test
    void testBuildSubscriptionPricingContext_postpaidPeriodDefinitions() {
        String subscriptionPaymentStrategy = DefaultSubscriptionPaymentStrategy.POSTPAID.name();
        String periodType = DefaultSubscriptionPeriodType.MONTHLY.name();

        Optional<SubscriptionWithItems> subscriptionWithItems = buildSubscriptionWithItems(subscriptionPaymentStrategy, periodType);

        Cart cart = buildCart(DefaultSubscriptionActionFlow.EDIT.name(),
                subscriptionWithItems.get().getSubscriptionId(),
                subscriptionPaymentStrategy, periodType);

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

        Instant expectedPeriod1StartDate = determinePreviousBillDate(period1.getBillDate(), periodType, 1);
        assertThat(period1.getBillDate()).isEqualTo(period1.getPeriodEndDate().plusNanos(1));
        assertThat(period1.getBillDate()).isEqualTo(atypicalNextBillDate);
        assertThat(period1.getPeriodStartDate()).isEqualTo(expectedPeriod1StartDate);
        assertThat(period1.getPeriodEndDate()).isEqualTo(period1.getBillDate().minusNanos(1));
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
        String subscriptionPaymentStrategy = DefaultSubscriptionPaymentStrategy.IN_ADVANCE.name();
        String periodType = DefaultSubscriptionPeriodType.MONTHLY.name();

        Optional<SubscriptionWithItems> subscriptionWithItems = buildSubscriptionWithItems(subscriptionPaymentStrategy, periodType);

        Cart cart = buildCart(DefaultSubscriptionActionFlow.EDIT.name(),
                subscriptionWithItems.get().getSubscriptionId(),
                subscriptionPaymentStrategy, periodType);

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

        Instant previousBillDate = determinePreviousBillDate(atypicalNextBillDate, periodType, 1);
        assertThat(period1.getBillDate()).isEqualTo(previousBillDate);
        assertThat(period1.getPeriodStartDate()).isEqualTo(previousBillDate);
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

    private Cart buildCart(String subscriptionActionFlow,
            String existingSubscriptionId,
            String subscriptionPaymentStrategy,
            String periodType) {
        return buildCart(false, subscriptionActionFlow, existingSubscriptionId, subscriptionPaymentStrategy, periodType);
    }

    private Cart buildCart(boolean includeDueNowPrice,
            String subscriptionActionFlow,
            String existingSubscriptionId,
            String subscriptionPaymentStrategy,
            String periodType) {
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
        parentItem.setTermDurationType(DefaultTermDurationType.YEARS.name());
        parentItem.setTermDurationLength(3);
        parentItem.getInternalAttributes().put(SUBSCRIPTION_ACTION_FLOW, subscriptionActionFlow);
        parentItem.getInternalAttributes().put(EXISTING_SUBSCRIPTION_ID, existingSubscriptionId);
        parentItem.getInternalAttributes().put(SUBSCRIPTION_PAYMENT_STRATEGY,
                subscriptionPaymentStrategy);

        RecurringPriceDetail parentRecurringPrice = new RecurringPriceDetail();
        parentRecurringPrice.setPrice(MonetaryUtils.toAmount(29.00, "USD"));
        parentRecurringPrice.setPeriodType(periodType);
        parentRecurringPrice.setPeriodFrequency(1);

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
        childItem.setTermDurationType(DefaultTermDurationType.YEARS.name());
        childItem.setTermDurationLength(3);

        RecurringPriceDetail childRecurringPrice = new RecurringPriceDetail();
        childRecurringPrice.setPrice(MonetaryUtils.toAmount(10.00, "USD"));
        childRecurringPrice.setPeriodType(periodType);
        childRecurringPrice.setPeriodFrequency(1);

        childItem.setRecurringPrice(childRecurringPrice);
        parentItem.getDependentCartItems().add(childItem);

        return cart;
    }

    private Optional<SubscriptionWithItems> buildSubscriptionWithItems(String subscriptionPaymentStrategy,
            String periodType) {
        String subscriptionId = ULID.random();
        String parentSubItemId = ULID.random();
        String childSubItemId = ULID.random();
        String customerId = ULID.random();
        String parentProductId = ULID.random();
        String childProductId = ULID.random();

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setName("1 Month 8 GB Phone Plan");
        subscription.setSubscriptionStatus(SubscriptionStatuses.ACTIVE.name());
        subscription.setRootItemRefType(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        subscription.setRootItemRef(parentProductId);
        subscription.setUserRefType(DefaultUserRefTypes.BLC_CUSTOMER.name());
        subscription.setUserRef(customerId);
        subscription.setPeriodType(periodType);
        subscription.setPeriodFrequency(1);
        subscription.setPaymentStrategy(subscriptionPaymentStrategy);
        subscription.setNextBillDate(getDateXDaysFromToday(17));
        subscription.setEndOfTermsDate(addMonths(subscription.getNextBillDate(), 29));
        subscription.setNextPeriod(8);
        subscription.setVersion(27);
        subscription.setCurrency(MonetaryUtils.getCurrency("USD"));

        SubscriptionItem parentItem = new SubscriptionItem();
        parentItem.setId(parentSubItemId);
        parentItem.setSubscriptionId(subscriptionId);
        parentItem.setItemName("1 Month 8 GB Phone Plan");
        parentItem.setItemRefType(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        parentItem.setItemRef(parentProductId);
        parentItem.setItemUnitPrice(BigDecimal.valueOf(29.00));
        parentItem.setQuantity(1);

        SubscriptionItem childItem = new SubscriptionItem();
        childItem.setId(childSubItemId);
        childItem.setSubscriptionId(subscriptionId);
        childItem.setItemName("Premium Entertainment Package");
        childItem.setParentItemRefType(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        childItem.setParentItemRef(parentProductId);
        childItem.setItemRefType(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        childItem.setItemRef(childProductId);
        childItem.setItemUnitPrice(BigDecimal.valueOf(10.00));
        childItem.setQuantity(1);

        SubscriptionWithItems subscriptionWithItems = new SubscriptionWithItems();
        subscriptionWithItems.setSubscription(subscription);
        subscriptionWithItems.getSubscriptionItems().add(parentItem);
        subscriptionWithItems.getSubscriptionItems().add(childItem);

        return Optional.of(subscriptionWithItems);
    }

    private Date getDateXDaysFromToday(int daysFromToday) {
        Instant xDaysFromToday = Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .plus(daysFromToday, ChronoUnit.DAYS);

        return Date.from(xDaysFromToday);
    }

    private Date addMonths(Date startDate, int monthsToAdd) {
        LocalDateTime ldt = startDate.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();

        return Date.from(ldt.plusMonths(monthsToAdd)
                .atZone(ZoneOffset.UTC)
                .toInstant());
    }

    protected Instant determinePreviousBillDate(Instant billDate, String periodType, int periodFrequency) {
        int monthsToSubtract;

        if (isMonthly(periodType)) {
            monthsToSubtract = periodFrequency;
        } else if (isQuarterly(periodType)) {
            monthsToSubtract = periodFrequency * 3;
        } else if (isAnnually(periodType)) {
            monthsToSubtract = periodFrequency * 12;
        } else {
            throw new IllegalArgumentException(
                    String.format("Invalid period type (%s).", periodType));
        }

        return subtractMonths(billDate, monthsToSubtract);
    }

    protected Instant subtractMonths(@lombok.NonNull Instant startDate, int monthsToAdd) {
        LocalDateTime ldt = startDate.atZone(ZoneOffset.UTC).toLocalDateTime();

        return ldt.minusMonths(monthsToAdd)
                .atZone(ZoneOffset.UTC)
                .toInstant();
    }
}
