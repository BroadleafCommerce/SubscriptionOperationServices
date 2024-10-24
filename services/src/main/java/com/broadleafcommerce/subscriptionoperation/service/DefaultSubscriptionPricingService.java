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
package com.broadleafcommerce.subscriptionoperation.service;

import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.EXISTING_SUBSCRIPTION_ID;
import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.EXISTING_SUBSCRIPTION_NEXT_BILL_DATE;
import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.IS_SEPARATE_FROM_PRIMARY_ITEM;
import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.SUBSCRIPTION_ACTION_FLOW;
import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.SUBSCRIPTION_PAYMENT_STRATEGY;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow.isCreate;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPaymentStrategy.isInAdvance;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPaymentStrategy.isPostpaid;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isAnnually;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isMonthly;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isQuarterly;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.cart.client.domain.CartItem;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.money.util.MonetaryUtils;
import com.broadleafcommerce.order.common.domain.RecurringPriceDetail;
import com.broadleafcommerce.subscriptionoperation.domain.EstimatedFuturePayment;
import com.broadleafcommerce.subscriptionoperation.domain.PeriodDefinition;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceItemDetail;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceResponse;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPricingContext;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionItemReferenceType;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultTermDurationType;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.money.MonetaryAmount;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultSubscriptionPricingService implements SubscriptionPricingService {

    @Getter(AccessLevel.PROTECTED)
    protected final SubscriptionProvider<SubscriptionWithItems> subscriptionProvider;

    @Getter(AccessLevel.PROTECTED)
    protected final TypeFactory typeFactory;

    @Override
    public List<SubscriptionPriceResponse> priceSubscriptions(@lombok.NonNull Cart cart,
            @Nullable ContextInfo contextInfo) {
        return identifySubscriptionRootItems(cart, contextInfo)
                .map(subscriptionRootItem -> priceSubscription(subscriptionRootItem, contextInfo))
                .toList();
    }

    protected Stream<CartItem> identifySubscriptionRootItems(@lombok.NonNull Cart cart,
            @Nullable ContextInfo contextInfo) {
        List<CartItem> cartItemsToConsider = cart.getCartItems();

        cart.getCartItems().stream()
                .filter(this::isSeparateFromPrimaryItem)
                .flatMap(cartItem -> cartItem.getDependentCartItems().stream())
                .forEach(cartItemsToConsider::add);

        return cartItemsToConsider.stream()
                .filter(this::hasRecurringPriceConfiguration);
    }

    protected boolean isSeparateFromPrimaryItem(@lombok.NonNull CartItem cartItem) {
        return MapUtils.getBooleanValue(cartItem.getInternalAttributes(),
                IS_SEPARATE_FROM_PRIMARY_ITEM, false);
    }

    protected boolean hasRecurringPriceConfiguration(@lombok.NonNull CartItem cartItem) {
        return Optional.of(cartItem)
                .map(CartItem::getRecurringPrice)
                .map(RecurringPriceDetail::getPeriodType)
                .isPresent();
    }

    protected SubscriptionPriceResponse priceSubscription(
            @lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        SubscriptionPricingContext pricingContext =
                buildSubscriptionPricingContext(subscriptionRootItem, contextInfo);

        SubscriptionPriceResponse response = typeFactory.get(SubscriptionPriceResponse.class);
        response.setRootItemRefType(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        response.setRootItemRef(subscriptionRootItem.getProductId());
        response.setRootCartItemId(subscriptionRootItem.getId());

        response = populateDueNowDetails(response, subscriptionRootItem, pricingContext,
                contextInfo);

        List<EstimatedFuturePayment> estimatedFuturePayments = buildEstimatedFuturePayments(
                subscriptionRootItem, pricingContext, contextInfo);
        response.setEstimatedFuturePayments(estimatedFuturePayments);

        return response;
    }

    protected SubscriptionPricingContext buildSubscriptionPricingContext(
            @lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        SubscriptionPricingContext context = typeFactory.get(SubscriptionPricingContext.class);
        context.setFlow(getSubscriptionActionFlow(subscriptionRootItem, contextInfo));
        context.setCurrency(subscriptionRootItem.getCurrency());

        Optional<SubscriptionWithItems> existingSubscription =
                getExistingSubscription(subscriptionRootItem, contextInfo);
        if (existingSubscription.isPresent()) {
            populateFromExistingSubscription(context, existingSubscription.get(), contextInfo);
        } else {
            populateFromCartItem(context, subscriptionRootItem, contextInfo);
        }

        Map<Integer, PeriodDefinition> periodDefinitions =
                buildPeriodDefinitions(subscriptionRootItem, context, contextInfo);
        context.setPeriodDefinitions(periodDefinitions);

        return context;
    }

    protected String getSubscriptionActionFlow(@lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        return MapUtils.getString(subscriptionRootItem.getInternalAttributes(),
                SUBSCRIPTION_ACTION_FLOW, DefaultSubscriptionActionFlow.CREATE.name());
    }

    protected Optional<SubscriptionWithItems> getExistingSubscription(
            @lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        String existingSubscriptionId = MapUtils
                .getString(subscriptionRootItem.getInternalAttributes(), EXISTING_SUBSCRIPTION_ID);

        if (StringUtils.isBlank(existingSubscriptionId)) {
            return Optional.empty();
        }

        return subscriptionProvider.readSubscriptionById(existingSubscriptionId, contextInfo);
    }

    protected void populateFromExistingSubscription(
            @lombok.NonNull SubscriptionPricingContext context,
            @lombok.NonNull SubscriptionWithItems existingSubscriptionWithItems,
            @Nullable ContextInfo contextInfo) {
        Subscription existingSubscription = existingSubscriptionWithItems.getSubscription();

        context.setExistingSubscriptionId(existingSubscription.getId());
        context.setPaymentStrategy(existingSubscription.getPaymentStrategy());
        context.setPaymentStrategy(existingSubscription.getPaymentStrategy());
        context.setPeriodType(existingSubscription.getPeriodType());
        context.setPeriodFrequency(existingSubscription.getPeriodFrequency());
        context.setEndOfTermsDate(existingSubscription.getEndOfTermsDate().toInstant());
        context.setAtypicalNextBillDate(existingSubscription.getNextBillDate().toInstant());
    }

    protected void populateFromCartItem(@lombok.NonNull SubscriptionPricingContext context,
            @lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        context.setPaymentStrategy(
                getSubscriptionPaymentStrategy(subscriptionRootItem, contextInfo));

        RecurringPriceDetail recurringPriceDetail = subscriptionRootItem.getRecurringPrice();
        context.setPeriodType(recurringPriceDetail.getPeriodType());
        context.setPeriodFrequency(recurringPriceDetail.getPeriodFrequency());

        Instant endOfTermsDate = determineEndOfTermsDate(subscriptionRootItem.getTermDurationType(),
                subscriptionRootItem.getTermDurationLength());
        context.setEndOfTermsDate(endOfTermsDate);

        Instant atypicalNextBillDate =
                determineAtypicalNextBillDate(subscriptionRootItem, context, contextInfo);
        context.setAtypicalNextBillDate(atypicalNextBillDate);
    }

    protected String getSubscriptionPaymentStrategy(@lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        String subscriptionPaymentStrategy = MapUtils.getString(
                subscriptionRootItem.getInternalAttributes(), SUBSCRIPTION_PAYMENT_STRATEGY);

        if (StringUtils.isBlank(subscriptionPaymentStrategy)) {
            throw new IllegalArgumentException(String.format(
                    "The subscription payment strategy could not be identified for cart item id: %s & product Id: %s.",
                    subscriptionRootItem.getId(), subscriptionRootItem.getProductId()));
        }

        return subscriptionPaymentStrategy;
    }

    protected Instant determineEndOfTermsDate(@lombok.NonNull String termDurationType,
            @lombok.NonNull Integer termDurationLength) {
        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime ldt = today.atZone(ZoneOffset.UTC).toLocalDateTime();

        if (DefaultTermDurationType.isDays(termDurationType)) {
            ldt.plusDays(termDurationLength);
        } else if (DefaultTermDurationType.isWeeks(termDurationType)) {
            ldt.plusWeeks(termDurationLength);
        } else if (DefaultTermDurationType.isMonths(termDurationType)) {
            ldt.plusMonths(termDurationLength);
        } else if (DefaultTermDurationType.isYears(termDurationType)) {
            ldt.plusYears(termDurationLength);
        }

        return ldt.atZone(ZoneOffset.UTC).toInstant();
    }

    /**
     * Returns a non-typical next bill date for the subscription or null if a date is not
     * applicable. The assumption is that this date is sometime in the future.
     *
     * @param subscriptionRootItem The {@link CartItem} that represents the root of the subscription
     *        items
     * @param pricingContext The {@link SubscriptionPricingContext} in which this operation is
     *        occurring
     * @param contextInfo context surrounding the multi-tenant state
     * @return a non-typical next bill date for the subscription or null if one is not applicable.
     */
    protected Instant determineAtypicalNextBillDate(@lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        if (!isCreate(pricingContext.getFlow())) {
            return pricingContext.getAtypicalNextBillDate();
        }

        return null;
    }

    protected Instant getExistingSubscriptionNextBillDate(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        return (Instant) MapUtils.getObject(subscriptionRootItem.getInternalAttributes(),
                EXISTING_SUBSCRIPTION_NEXT_BILL_DATE);
    }

    protected Map<Integer, PeriodDefinition> buildPeriodDefinitions(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        Map<Integer, PeriodDefinition> estimatedFuturePaymentPeriods = new HashMap<>();

        Instant previousBillDate = Optional.ofNullable(pricingContext.getAtypicalNextBillDate())
                .map(atypicalNextBillDate -> atypicalNextBillDate.truncatedTo(ChronoUnit.DAYS))
                .orElse(Instant.now().truncatedTo(ChronoUnit.DAYS));

        int estimatedFuturePaymentCount =
                getEstimatedFuturePaymentCount(subscriptionRootItem, pricingContext, contextInfo);
        for (int period = 1; period <= estimatedFuturePaymentCount; period++) {
            if (period == 1 && pricingContext.getAtypicalNextBillDate() != null) {
                PeriodDefinition periodDefinition = buildAtypicalFirstPeriodDefinition(
                        estimatedFuturePaymentPeriods, period, pricingContext, contextInfo);

                previousBillDate = periodDefinition.getBillDate();
                continue;
            }

            PeriodDefinition periodDefinition = typeFactory.get(PeriodDefinition.class);
            Instant billDate =
                    determineNextBillDate(period, previousBillDate, pricingContext, contextInfo);
            periodDefinition.setBillDate(billDate);

            Instant periodStartDate =
                    determinePeriodStartDate(previousBillDate, billDate, pricingContext,
                            contextInfo);
            periodDefinition.setPeriodStartDate(periodStartDate);

            Instant periodEndDate =
                    determinePeriodEndDate(period, periodDefinition, pricingContext, contextInfo);
            periodDefinition.setPeriodEndDate(periodEndDate);

            estimatedFuturePaymentPeriods.put(period, periodDefinition);
            previousBillDate = periodDefinition.getBillDate();
        }

        return estimatedFuturePaymentPeriods;
    }

    protected PeriodDefinition buildAtypicalFirstPeriodDefinition(
            @lombok.NonNull Map<Integer, PeriodDefinition> estimatedFuturePaymentPeriods,
            int period,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        PeriodDefinition periodDefinition = typeFactory.get(PeriodDefinition.class);
        Instant billDate = pricingContext.getAtypicalNextBillDate()
                .truncatedTo(ChronoUnit.DAYS);

        if (!isCreate(pricingContext.getFlow())
                && isInAdvance(pricingContext.getPaymentStrategy())) {
            billDate = determinePreviousBillDate(billDate, pricingContext, contextInfo);
            periodDefinition.setBillDate(billDate);
        } else {
            periodDefinition.setBillDate(billDate);
        }

        if (isInAdvance(pricingContext.getPaymentStrategy())) {
            periodDefinition.setPeriodStartDate(billDate);
        } else if (isPostpaid(pricingContext.getPaymentStrategy())) {
            if (isCreate(pricingContext.getFlow())) {
                Instant beginningOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
                periodDefinition.setPeriodStartDate(beginningOfToday);
            } else {
                Instant periodStartDate =
                        determinePreviousBillDate(billDate, pricingContext, contextInfo);
                periodDefinition.setPeriodStartDate(periodStartDate);
            }
        }

        Instant periodEndDate =
                determinePeriodEndDate(period, periodDefinition, pricingContext,
                        contextInfo);
        periodDefinition.setPeriodEndDate(periodEndDate);

        estimatedFuturePaymentPeriods.put(period, periodDefinition);
        return periodDefinition;
    }

    protected int getEstimatedFuturePaymentCount(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        return 12;
    }

    protected Instant determineNextBillDate(int period,
            @lombok.NonNull Instant startDate,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        int monthsToAdd;

        String periodType = pricingContext.getPeriodType();
        if (isMonthly(periodType)) {
            monthsToAdd = pricingContext.getPeriodFrequency();
        } else if (isQuarterly(periodType)) {
            monthsToAdd = pricingContext.getPeriodFrequency() * 3;
        } else if (isAnnually(periodType)) {
            monthsToAdd = pricingContext.getPeriodFrequency() * 12;
        } else {
            throw new IllegalArgumentException(
                    String.format("Invalid period type (%s).", periodType));
        }

        return addMonths(startDate, monthsToAdd);
    }

    protected Instant addMonths(@lombok.NonNull Instant startDate, int monthsToAdd) {
        LocalDateTime ldt = startDate.atZone(ZoneOffset.UTC).toLocalDateTime();

        return ldt.plusMonths(monthsToAdd)
                .atZone(ZoneOffset.UTC)
                .toInstant();
    }

    protected Instant determinePreviousBillDate(@lombok.NonNull Instant billDate,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        int monthsToSubtract;

        String periodType = pricingContext.getPeriodType();
        if (isMonthly(periodType)) {
            monthsToSubtract = pricingContext.getPeriodFrequency();
        } else if (isQuarterly(periodType)) {
            monthsToSubtract = pricingContext.getPeriodFrequency() * 3;
        } else if (isAnnually(periodType)) {
            monthsToSubtract = pricingContext.getPeriodFrequency() * 12;
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

    protected Instant determinePeriodStartDate(@lombok.NonNull Instant previousBillDate,
            @lombok.NonNull Instant billDate,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        if (isPostpaid(pricingContext.getPaymentStrategy())) {
            return previousBillDate;
        } else if (isInAdvance(pricingContext.getPaymentStrategy())) {
            return billDate;
        } else {
            throw new IllegalArgumentException(
                    String.format("Unexpected subscription pricing strategy: %s",
                            pricingContext.getPaymentStrategy()));
        }
    }

    protected Instant determinePeriodEndDate(int period,
            PeriodDefinition periodDefinition,
            SubscriptionPricingContext pricingContext,
            ContextInfo contextInfo) {
        Instant nextBillDate;

        if (isPostpaid(pricingContext.getPaymentStrategy())) {
            nextBillDate = periodDefinition.getBillDate();
        } else if (isInAdvance(pricingContext.getPaymentStrategy())) {
            nextBillDate = determineNextBillDate(period + 1, periodDefinition.getBillDate(),
                    pricingContext,
                    contextInfo);
        } else {
            throw new IllegalArgumentException(
                    String.format("Unexpected subscription pricing strategy: %s",
                            pricingContext.getPaymentStrategy()));
        }

        return getEndOfPreviousDay(nextBillDate);
    }

    protected Instant getEndOfPreviousDay(@lombok.NonNull Instant nextBillDate) {
        return nextBillDate.truncatedTo(ChronoUnit.DAYS)
                .minusNanos(1);
    }

    protected SubscriptionPriceResponse populateDueNowDetails(
            @lombok.NonNull SubscriptionPriceResponse response,
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        List<SubscriptionPriceItemDetail> dueNowItemDetails = buildDueNowItemDetails(
                subscriptionRootItem, pricingContext, contextInfo);
        response.setDueNowItemDetails(dueNowItemDetails);

        MonetaryAmount dueNowProratedAmount =
                calculateProratedAmountTotal(dueNowItemDetails, pricingContext,
                        contextInfo);
        response.setProratedAmount(dueNowProratedAmount);

        MonetaryAmount dueNowCreditedAmount =
                calculateCreditedAmountTotal(dueNowItemDetails, pricingContext,
                        contextInfo);
        response.setCreditedAmount(dueNowCreditedAmount);

        MonetaryAmount dueNowPriorUnbilledAmount =
                calculatePriorUnbilledAmountTotal(dueNowItemDetails,
                        pricingContext, contextInfo);
        response.setPriorUnbilledAmount(dueNowPriorUnbilledAmount);

        MonetaryAmount amountDueNow = dueNowProratedAmount.add(dueNowPriorUnbilledAmount)
                .subtract(dueNowCreditedAmount);
        response.setAmountDueNow(amountDueNow);

        return response;
    }

    protected List<SubscriptionPriceItemDetail> buildDueNowItemDetails(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        return buildItemDetails(subscriptionRootItem, null, pricingContext, contextInfo);
    }

    protected List<SubscriptionPriceItemDetail> buildItemDetails(
            @lombok.NonNull CartItem subscriptionRootItem,
            @Nullable Integer period,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        List<SubscriptionPriceItemDetail> itemDetails = new ArrayList<>();

        SubscriptionPriceItemDetail itemDetail =
                buildItemDetailForCartItem(subscriptionRootItem, null, period, pricingContext,
                        contextInfo);
        itemDetails.add(itemDetail);

        itemDetails.addAll(buildItemDetailsForDependentCartItems(subscriptionRootItem, period,
                pricingContext, contextInfo));
        return itemDetails;
    }

    protected SubscriptionPriceItemDetail buildItemDetailForCartItem(CartItem cartItem,
            @Nullable CartItem parentCartItem,
            @Nullable Integer period,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        RecurringPriceDetail recurringPriceDetail = cartItem.getRecurringPrice();
        if (!hasMatchingBillingFrequency(recurringPriceDetail, pricingContext)) {
            throw new IllegalArgumentException(String.format(
                    "Each subscription item's RecurringPriceDetail must have the same billing frequency as the overall "
                            +
                            "subscription. Subscription period type & frequency: %s & %s. Item period type & frequency: %s & %s.",
                    pricingContext.getPeriodType(), pricingContext.getPeriodFrequency(),
                    recurringPriceDetail.getPeriodType(),
                    recurringPriceDetail.getPeriodFrequency()));
        }

        SubscriptionPriceItemDetail itemDetail =
                typeFactory.get(SubscriptionPriceItemDetail.class);
        itemDetail.setItemRefType(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        itemDetail.setItemRef(cartItem.getProductId());
        itemDetail.setCartItemId(cartItem.getId());

        MonetaryAmount proratedAmount =
                determineProratedAmount(cartItem, period, pricingContext, contextInfo);
        itemDetail.setProratedAmount(proratedAmount);

        MonetaryAmount creditedAmount =
                determineCreditedAmount(cartItem, period, pricingContext, contextInfo);
        itemDetail.setCreditedAmount(creditedAmount);

        MonetaryAmount priorUnbilledAmount =
                determinePriorUnbilledAmount(cartItem, period, pricingContext, contextInfo);
        itemDetail.setPriorUnbilledAmount(priorUnbilledAmount);

        MonetaryAmount amount = proratedAmount.add(priorUnbilledAmount)
                .subtract(creditedAmount);
        itemDetail.setAmount(amount);

        if (parentCartItem != null) {
            itemDetail
                    .setParentItemRefType(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
            itemDetail.setParentItemRef(parentCartItem.getProductId());
        }

        return itemDetail;
    }

    protected boolean hasMatchingBillingFrequency(
            @lombok.NonNull RecurringPriceDetail recurringPriceDetail,
            @lombok.NonNull SubscriptionPricingContext pricingContext) {
        return Objects.equals(recurringPriceDetail.getPeriodType(),
                pricingContext.getPeriodType())
                && Objects.equals(recurringPriceDetail.getPeriodFrequency(),
                        pricingContext.getPeriodFrequency());
    }

    protected List<SubscriptionPriceItemDetail> buildItemDetailsForDependentCartItems(
            @lombok.NonNull CartItem cartItem,
            @Nullable Integer period,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        return cartItem.getDependentCartItems().stream()
                .filter(doi -> !isSeparateFromPrimaryItem(doi))
                .map(doi -> buildItemDetailForCartItem(doi, cartItem, period, pricingContext,
                        contextInfo))
                .toList();
    }

    protected MonetaryAmount determineProratedAmount(@NonNull CartItem cartItem,
            @Nullable Integer period,
            @NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        RecurringPriceDetail recurringPriceDetail = cartItem.getRecurringPrice();
        MonetaryAmount typicalRecurringPrice = recurringPriceDetail.getPrice();

        if (isCreate(pricingContext.getFlow())) {
            return determineProratedAmountForCreateFlow(cartItem, period, pricingContext,
                    contextInfo);
        } else {
            // TODO: This needs more logic!
            return typicalRecurringPrice;
        }
    }

    protected MonetaryAmount determineProratedAmountForCreateFlow(@lombok.NonNull CartItem cartItem,
            @Nullable Integer period,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        RecurringPriceDetail recurringPriceDetail = cartItem.getRecurringPrice();
        MonetaryAmount typicalRecurringPrice = recurringPriceDetail.getPrice();

        if (period == null) {
            // Up-front cost
            return cartItem.getSubtotal();
        }

        if (period == 1 && isPostpaid(pricingContext.getPaymentStrategy())) {
            if (pricingContext.getAtypicalNextBillDate() == null) {
                return typicalRecurringPrice;
            }

            MonetaryAmount pricePerDay =
                    determinePricePerDay(cartItem, pricingContext, contextInfo);
            PeriodDefinition periodDefinition =
                    pricingContext.getPeriodDefinition(period);
            long daysToBill = getDaysToBill(periodDefinition);

            return MonetaryUtils.round(pricePerDay.multiply(daysToBill));
        } else {
            return typicalRecurringPrice;
        }
    }

    protected long getDaysToBill(PeriodDefinition periodDefinition) {
        Instant beginningOfStartDate = periodDefinition.getPeriodStartDate()
                .truncatedTo(ChronoUnit.DAYS);

        // Period end date expected to have time similar to 23:59:59
        // Shift to the beginning of the next day to get an accurate full day count
        Instant endOfEndDate = periodDefinition.getPeriodEndDate()
                .plus(1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS);

        return ChronoUnit.DAYS.between(beginningOfStartDate, endOfEndDate);
    }

    protected MonetaryAmount determinePricePerDay(@lombok.NonNull CartItem cartItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        RecurringPriceDetail recurringPriceDetail = cartItem.getRecurringPrice();
        MonetaryAmount typicalRecurringPrice = recurringPriceDetail.getPrice();

        long numberOfDaysInPeriod =
                determineNumberOfDaysInPeriod(cartItem, pricingContext, contextInfo);

        return typicalRecurringPrice.divide(numberOfDaysInPeriod);
    }

    protected long determineNumberOfDaysInPeriod(@lombok.NonNull CartItem cartItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        if (isMonthly(pricingContext.getPeriodType())) {
            return 30L * pricingContext.getPeriodFrequency();
        } else if (isQuarterly(pricingContext.getPeriodType())) {
            return 90L * pricingContext.getPeriodFrequency();
        } else if (isAnnually(pricingContext.getPeriodType())) {
            return 365L * pricingContext.getPeriodFrequency();
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unable to determine number of days in the period. Period type: %s.",
                    pricingContext.getPeriodType()));
        }
    }

    protected MonetaryAmount determineCreditedAmount(@NonNull CartItem cartItem,
            @Nullable Integer period,
            @NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        if (!isCreate(pricingContext.getFlow())
                && isInAdvance(pricingContext.getPaymentStrategy())) {
            // TODO: Need previous subscription price from cart item
        }

        return MonetaryUtils.zero(pricingContext.getCurrency());
    }

    protected MonetaryAmount determinePriorUnbilledAmount(@NonNull CartItem cartItem,
            @Nullable Integer period,
            @NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        if (!isCreate(pricingContext.getFlow())
                && isPostpaid(pricingContext.getPaymentStrategy())) {
            // TODO: Need previous subscription price from cart item
        }

        return MonetaryUtils.zero(pricingContext.getCurrency());
    }

    protected MonetaryAmount calculateProratedAmountTotal(
            @lombok.NonNull List<SubscriptionPriceItemDetail> itemDetails,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        MonetaryAmount zero = MonetaryUtils.zero(pricingContext.getCurrency());

        return itemDetails.stream()
                .map(SubscriptionPriceItemDetail::getProratedAmount)
                .reduce(zero, MonetaryAmount::add);
    }

    protected MonetaryAmount calculateCreditedAmountTotal(
            @lombok.NonNull List<SubscriptionPriceItemDetail> itemDetails,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        MonetaryAmount zero = MonetaryUtils.zero(pricingContext.getCurrency());

        return itemDetails.stream()
                .map(SubscriptionPriceItemDetail::getCreditedAmount)
                .reduce(zero, MonetaryAmount::add);
    }

    protected MonetaryAmount calculatePriorUnbilledAmountTotal(
            @lombok.NonNull List<SubscriptionPriceItemDetail> itemDetails,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        MonetaryAmount zero = MonetaryUtils.zero(pricingContext.getCurrency());

        return itemDetails.stream()
                .map(SubscriptionPriceItemDetail::getPriorUnbilledAmount)
                .reduce(zero, MonetaryAmount::add);
    }

    protected List<EstimatedFuturePayment> buildEstimatedFuturePayments(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        List<EstimatedFuturePayment> estimatedFuturePayments = new ArrayList<>();

        pricingContext.getPeriodDefinitions().forEach((period, periodDefinition) -> {
            EstimatedFuturePayment estimatedFuturePayment =
                    buildEstimatedFuturePayment(subscriptionRootItem, period, pricingContext,
                            contextInfo);

            estimatedFuturePayments.add(estimatedFuturePayment);
        });

        return estimatedFuturePayments;
    }

    protected EstimatedFuturePayment buildEstimatedFuturePayment(
            @lombok.NonNull CartItem subscriptionRootItem,
            @Nullable Integer period,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        EstimatedFuturePayment estimatedFuturePayment =
                typeFactory.get(EstimatedFuturePayment.class);
        populatePeriodAndBillDates(estimatedFuturePayment, subscriptionRootItem, period,
                pricingContext, contextInfo);

        List<SubscriptionPriceItemDetail> itemDetails = buildItemDetails(
                subscriptionRootItem, period, pricingContext, contextInfo);
        estimatedFuturePayment.setItemDetails(itemDetails);

        MonetaryAmount proratedAmount =
                calculateProratedAmountTotal(itemDetails, pricingContext, contextInfo);
        estimatedFuturePayment.setProratedAmount(proratedAmount);

        MonetaryAmount creditedAmount =
                calculateCreditedAmountTotal(itemDetails, pricingContext, contextInfo);
        estimatedFuturePayment.setCreditedAmount(creditedAmount);

        MonetaryAmount priorUnbilledAmount =
                calculatePriorUnbilledAmountTotal(itemDetails, pricingContext, contextInfo);
        estimatedFuturePayment.setPriorUnbilledAmount(priorUnbilledAmount);

        MonetaryAmount amountDueNow = proratedAmount.add(priorUnbilledAmount)
                .subtract(creditedAmount);
        estimatedFuturePayment.setAmount(amountDueNow);

        return estimatedFuturePayment;
    }

    protected void populatePeriodAndBillDates(
            @lombok.NonNull EstimatedFuturePayment estimatedFuturePayment,
            @lombok.NonNull CartItem subscriptionRootItem,
            @Nullable Integer period,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        PeriodDefinition periodDefinition = pricingContext.getPeriodDefinition(period);

        estimatedFuturePayment.setBillDate(periodDefinition.getBillDate());
        estimatedFuturePayment.setPeriodStartDate(periodDefinition.getPeriodStartDate());
        estimatedFuturePayment.setPeriodEndDate(periodDefinition.getPeriodEndDate());
    }
}
