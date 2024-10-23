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
import static com.broadleafcommerce.subscriptionoperation.domain.constants.CartItemAttributeConstants.Internal.SUBSCRIPTION_PRICING_STRATEGY;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow.isCreate;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isAnnually;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isMonthly;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPeriodType.isQuarterly;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPricingStrategy.isInAdvance;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionPricingStrategy.isPostpaid;

import org.apache.commons.collections4.MapUtils;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.cart.client.domain.CartItem;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.money.util.MonetaryUtils;
import com.broadleafcommerce.order.common.domain.RecurringPriceDetail;
import com.broadleafcommerce.subscriptionoperation.domain.EstimatedFuturePayment;
import com.broadleafcommerce.subscriptionoperation.domain.PeriodDefinition;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceItemDetail;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPriceResponse;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionPricingContext;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionItemReferenceType;

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

import io.micrometer.common.util.StringUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultSubscriptionPricingService implements SubscriptionPricingService {

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
        context.setExistingSubscriptionId(
                getExistingSubscriptionId(subscriptionRootItem, contextInfo));
        context.setPricingStrategy(
                getSubscriptionPricingStrategy(subscriptionRootItem, contextInfo));

        RecurringPriceDetail recurringPriceDetail = subscriptionRootItem.getRecurringPrice();
        context.setPeriodType(recurringPriceDetail.getPeriodType());
        context.setPeriodFrequency(recurringPriceDetail.getPeriodFrequency());
        context.setTermDurationType(subscriptionRootItem.getTermDurationType());
        context.setTermDurationLength(subscriptionRootItem.getTermDurationLength());
        context.setCurrency(subscriptionRootItem.getCurrency());

        Instant atypicalNextBillDate =
                determineAtypicalNextBillDate(subscriptionRootItem, context, contextInfo);
        context.setAtypicalNextBillDate(atypicalNextBillDate);

        Map<Integer, PeriodDefinition> periodDefinitions =
                buildPeriodDefinitions(subscriptionRootItem, context, contextInfo);
        context.setPeriodDefinitions(periodDefinitions);

        return context;
    }


    private String getSubscriptionActionFlow(@lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        return MapUtils.getString(subscriptionRootItem.getInternalAttributes(),
                SUBSCRIPTION_ACTION_FLOW, DefaultSubscriptionActionFlow.CREATE.name());
    }

    @Nullable
    private String getExistingSubscriptionId(@lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        return MapUtils.getString(subscriptionRootItem.getInternalAttributes(),
                EXISTING_SUBSCRIPTION_ID);
    }

    private String getSubscriptionPricingStrategy(@lombok.NonNull CartItem subscriptionRootItem,
            @Nullable ContextInfo contextInfo) {
        String subscriptionPricingStrategy = MapUtils.getString(
                subscriptionRootItem.getInternalAttributes(), SUBSCRIPTION_PRICING_STRATEGY);

        if (StringUtils.isBlank(subscriptionPricingStrategy)) {
            throw new IllegalArgumentException(String.format(
                    "The subscription pricing strategy could not be identified for cart item id: %s & product Id: %s.",
                    subscriptionRootItem.getId(), subscriptionRootItem.getProductId()));
        }

        return subscriptionPricingStrategy;
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

            // VFE Customization: remove isPostpaid(pricingContext.getPricingStrategy() from
            // if-condition
            if (period == 1 && pricingContext.getAtypicalNextBillDate() != null
                    && isPostpaid(pricingContext.getPricingStrategy())) {
                PeriodDefinition periodDefinition = typeFactory.get(PeriodDefinition.class);
                Instant billDate =
                        pricingContext.getAtypicalNextBillDate().truncatedTo(ChronoUnit.DAYS);
                periodDefinition.setBillDate(billDate);

                Instant beginningOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
                periodDefinition.setPeriodStartDate(beginningOfToday);

                Instant periodEndDate =
                        determinePeriodEndDate(period, periodDefinition, pricingContext,
                                contextInfo);
                periodDefinition.setPeriodEndDate(periodEndDate);

                estimatedFuturePaymentPeriods.put(period, periodDefinition);
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

    protected int getEstimatedFuturePaymentCount(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        return 12;
    }

    protected Instant determineNextBillDate(int period,
            @NonNull Instant startDate,
            @NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        // TODO: remove for VFE customization
        if (period == 1 && isInAdvance(pricingContext.getPricingStrategy())) {
            return startDate;
        }

        int monthsToAdd = 0;

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

    protected Instant determinePeriodStartDate(@lombok.NonNull Instant previousBillDate,
            @lombok.NonNull Instant billDate,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        if (isPostpaid(pricingContext.getPricingStrategy())) {
            return previousBillDate;
        } else if (isInAdvance(pricingContext.getPricingStrategy())) {
            return billDate;
        } else {
            throw new IllegalArgumentException(
                    String.format("Unexpected subscription pricing strategy: %s",
                            pricingContext.getPricingStrategy()));
        }
    }

    private Instant determinePeriodEndDate(int period,
            PeriodDefinition periodDefinition,
            SubscriptionPricingContext pricingContext,
            ContextInfo contextInfo) {
        Instant nextBillDate;

        if (isPostpaid(pricingContext.getPricingStrategy())) {
            nextBillDate = periodDefinition.getBillDate();
        } else if (isInAdvance(pricingContext.getPricingStrategy())) {
            nextBillDate = determineNextBillDate(period + 1, periodDefinition.getBillDate(),
                    pricingContext,
                    contextInfo);
        } else {
            throw new IllegalArgumentException(
                    String.format("Unexpected subscription pricing strategy: %s",
                            pricingContext.getPricingStrategy()));
        }

        return getEndOfPreviousDay(nextBillDate);
    }

    private Instant getEndOfPreviousDay(@lombok.NonNull Instant nextBillDate) {
        return nextBillDate.truncatedTo(ChronoUnit.DAYS)
                .minusNanos(1);
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
            return getSubscriptionNextBillDate(subscriptionRootItem, pricingContext, contextInfo);
        }

        return null;
    }

    protected Instant getSubscriptionNextBillDate(@lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        return (Instant) MapUtils.getObject(subscriptionRootItem.getInternalAttributes(),
                EXISTING_SUBSCRIPTION_NEXT_BILL_DATE);
    }

    private SubscriptionPriceResponse populateDueNowDetails(
            @lombok.NonNull SubscriptionPriceResponse response,
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        List<SubscriptionPriceItemDetail> dueNowItemDetails = buildItemDetails(
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

    protected List<SubscriptionPriceItemDetail> buildItemDetails(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        List<SubscriptionPriceItemDetail> itemDetails = new ArrayList<>();

        SubscriptionPriceItemDetail itemDetail =
                buildItemDetailForCartItem(subscriptionRootItem, null, pricingContext,
                        contextInfo);
        itemDetails.add(itemDetail);

        itemDetails.addAll(buildItemDetailsForDependentCartItems(subscriptionRootItem,
                pricingContext, contextInfo));
        return itemDetails;
    }

    protected SubscriptionPriceItemDetail buildItemDetailForCartItem(CartItem cartItem,
            @Nullable CartItem parentCartItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        SubscriptionPriceItemDetail itemDetail =
                typeFactory.get(SubscriptionPriceItemDetail.class);
        itemDetail.setItemRefType(DefaultSubscriptionItemReferenceType.BLC_PRODUCT.name());
        itemDetail.setItemRef(cartItem.getProductId());
        itemDetail.setCartItemId(cartItem.getId());

        MonetaryAmount proratedAmount =
                determineProratedAmount(cartItem, pricingContext, contextInfo);
        itemDetail.setProratedAmount(proratedAmount);

        MonetaryAmount creditedAmount =
                determineCreditedAmount(cartItem, pricingContext, contextInfo);
        itemDetail.setCreditedAmount(creditedAmount);

        MonetaryAmount priorUnbilledAmount =
                determinePriorUnbilledAmount(cartItem, pricingContext, contextInfo);
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

    private MonetaryAmount determineProratedAmount(@lombok.NonNull CartItem cartItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        RecurringPriceDetail recurringPriceDetail = cartItem.getRecurringPrice();

        if (!hasMatchingBillingFrequency(recurringPriceDetail, pricingContext)) {
            throw new IllegalArgumentException(String.format(
                    "Each subscription item's RecurringPriceDetail must have the same billing frequency as the overall subscription. Subscription period type & frequency: %s & %s. Item period type & frequency: %s & %s.",
                    pricingContext.getPeriodType(), pricingContext.getPeriodFrequency(),
                    recurringPriceDetail.getPeriodType(),
                    recurringPriceDetail.getPeriodFrequency()));
        }

        Integer period = pricingContext.getEstimatedFuturePaymentPeriod();
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

        // VFE Customization: Remove isPostpaid(pricingContext.getPricingStrategy()) from
        // if-condition
        if (period == 1 && isPostpaid(pricingContext.getPricingStrategy())) {
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

    protected boolean hasMatchingBillingFrequency(
            @lombok.NonNull RecurringPriceDetail recurringPriceDetail,
            @lombok.NonNull SubscriptionPricingContext pricingContext) {
        return Objects.equals(recurringPriceDetail.getPeriodType(),
                pricingContext.getPeriodType())
                && Objects.equals(recurringPriceDetail.getPeriodFrequency(),
                        pricingContext.getPeriodFrequency());
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

    private MonetaryAmount determineCreditedAmount(@lombok.NonNull CartItem cartItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        if (!isCreate(pricingContext.getFlow())
                && isInAdvance(pricingContext.getPricingStrategy())) {
            // TODO: Need previous subscription price from cart item
        }

        return MonetaryUtils.zero(pricingContext.getCurrency());
    }

    private MonetaryAmount determinePriorUnbilledAmount(@lombok.NonNull CartItem cartItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        if (!isCreate(pricingContext.getFlow())
                && isPostpaid(pricingContext.getPricingStrategy())) {
            // TODO: Need previous subscription price from cart item
        }

        return MonetaryUtils.zero(pricingContext.getCurrency());
    }

    protected List<SubscriptionPriceItemDetail> buildItemDetailsForDependentCartItems(
            @lombok.NonNull CartItem cartItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        return cartItem.getDependentCartItems().stream()
                .filter(doi -> !isSeparateFromPrimaryItem(doi))
                .map(doi -> buildItemDetailForCartItem(doi, cartItem, pricingContext,
                        contextInfo))
                .toList();
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

    private List<EstimatedFuturePayment> buildEstimatedFuturePayments(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        List<EstimatedFuturePayment> estimatedFuturePayments = new ArrayList<>();

        pricingContext.getPeriodDefinitions().forEach((period, periodDefinition) -> {
            pricingContext.setEstimatedFuturePaymentPeriod(period);

            EstimatedFuturePayment estimatedFuturePayment =
                    buildEstimatedFuturePayment(subscriptionRootItem, pricingContext, contextInfo);

            estimatedFuturePayments.add(estimatedFuturePayment);
        });

        return estimatedFuturePayments;
    }

    private EstimatedFuturePayment buildEstimatedFuturePayment(
            @lombok.NonNull CartItem subscriptionRootItem,
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        EstimatedFuturePayment estimatedFuturePayment =
                typeFactory.get(EstimatedFuturePayment.class);
        populatePeriodAndBillDates(estimatedFuturePayment, subscriptionRootItem, pricingContext,
                contextInfo);

        List<SubscriptionPriceItemDetail> itemDetails = buildItemDetails(
                subscriptionRootItem, pricingContext, contextInfo);
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
            @lombok.NonNull SubscriptionPricingContext pricingContext,
            @Nullable ContextInfo contextInfo) {
        Integer currentPeriod = pricingContext.getEstimatedFuturePaymentPeriod();
        PeriodDefinition periodDefinition = pricingContext.getPeriodDefinition(currentPeriod);

        estimatedFuturePayment.setBillDate(periodDefinition.getBillDate());
        estimatedFuturePayment.setPeriodStartDate(periodDefinition.getPeriodStartDate());
        estimatedFuturePayment.setPeriodEndDate(periodDefinition.getPeriodEndDate());
    }
}