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
package com.broadleafcommerce.subscriptionoperation.service.modification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.cart.client.domain.enums.DefaultCartTypes;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.policy.trackable.TrackablePolicyUtils;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionFlow;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType;
import com.broadleafcommerce.subscriptionoperation.service.provider.CartOperationProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.AddItemRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.CreateCartRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionResponse;
import com.broadleafcommerce.subscriptionoperation.web.domain.PriceCartRequest;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.azam.ulidj.ULID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Handles initiating the edit-{@link Subscription} flow.
 *
 * @author Nathan Moore (nathandmoore)
 */
@RequiredArgsConstructor
public class InitiateEditSubscriptionHandler extends AbstractModifySubscriptionHandler
        implements ModifySubscriptionHandler {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final String SUBSCRIPTION_ITEM_ID = "subscriptionItemId";
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String FLOW = "flow";

    @Getter(value = AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    @Getter(value = AccessLevel.PROTECTED)
    private final CartOperationProvider<Cart> cartOperationProvider;

    @Getter(AccessLevel.PROTECTED)
    private final MessageSource messageSource;

    @Getter(value = AccessLevel.PROTECTED, onMethod_ = @Nullable)
    @Setter(onMethod_ = @Autowired(required = false))
    private TrackablePolicyUtils policyUtils;

    @Override
    public boolean canHandle(@lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        return DefaultSubscriptionActionType.isEdit(request.getAction().getActionType());
    }

    @Override
    protected ModifySubscriptionResponse handleInternal(
            @lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        SubscriptionWithItems subscriptionWithItems = request.getSubscription();
        CreateCartRequest createCartRequest = buildCreateCartRequest(request, contextInfo);
        Cart cart = cartOperationProvider.createCart(createCartRequest, contextInfo);

        ModifySubscriptionResponse response = typeFactory.get(
                ModifySubscriptionResponse.class);
        response.setSubscription(subscriptionWithItems);
        response.setCart(cart);
        return response;
    }

    @Override
    protected String[] getRequiredPermissions(@lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        // TODO Implement this method
        return new String[] {"ALL_CUSTOMER_SUBSCRIPTION", "ALL_ACCOUNT_SUBSCRIPTION",
                "EDIT_SUBSCRIPTION"};
    }

    protected CreateCartRequest buildCreateCartRequest(
            @lombok.NonNull ModifySubscriptionRequest request,
            @Nullable ContextInfo contextInfo) {
        SubscriptionWithItems subscriptionWithItems = request.getSubscription();
        Subscription subscription = subscriptionWithItems.getSubscription();
        List<SubscriptionItem> items = subscriptionWithItems.getSubscriptionItems();

        CreateCartRequest createCartRequest = typeFactory.get(CreateCartRequest.class);
        createCartRequest.setName(generateCartName(subscription));
        createCartRequest.setType(DefaultCartTypes.STANDARD.name());
        createCartRequest.setPriceCartRequest(buildPriceCartRequest(subscription, contextInfo));

        Map<String, List<String>> subItemIdsToChildrenIds = new HashMap<>();
        Map<String, AddItemRequest> addItemRequestsBySubItemId = new HashMap<>();
        AtomicReference<String> rootItemId = new AtomicReference<>();
        for (SubscriptionItem next : items) {
            List<String> children = items.stream()
                    .filter(item -> next.getItemRef().equals(item.getParentItemRef()))
                    .map(SubscriptionItem::getId)
                    .collect(Collectors.toList());
            subItemIdsToChildrenIds.put(next.getId(), children);
            AddItemRequest itemRequest = buildAddItemRequest(next, subscription);
            addItemRequestsBySubItemId.put(next.getId(), itemRequest);

            if (next.getParentItemRef() == null) {
                rootItemId.set(next.getId());
            }
        }

        addDependentsToParents(subItemIdsToChildrenIds, addItemRequestsBySubItemId);

        AddItemRequest rootItem = addItemRequestsBySubItemId.get(rootItemId.get());
        createCartRequest.getAddItemRequests().add(rootItem);

        return createCartRequest;
    }

    protected PriceCartRequest buildPriceCartRequest(@lombok.NonNull Subscription subscription,
            @Nullable ContextInfo contextInfo) {
        PriceCartRequest priceCartRequest = typeFactory.get(PriceCartRequest.class);
        priceCartRequest.setCurrency(subscription.getCurrency());
        return priceCartRequest;
    }

    protected void addDependentsToParents(
            @lombok.NonNull Map<String, List<String>> subItemIdsToChildrenIds,
            @lombok.NonNull Map<String, AddItemRequest> addItemRequestsBySubItemId) {
        subItemIdsToChildrenIds.forEach((id, children) -> {
            AddItemRequest parent = addItemRequestsBySubItemId.get(id);
            children.stream()
                    .map(addItemRequestsBySubItemId::get)
                    .forEach(addItemRequest -> parent.getDependentCartItems().add(addItemRequest));
        });
    }

    protected String generateCartName(Subscription subscription) {
        return "Edit %s - %s".formatted(subscription.getName(), ULID.random(SECURE_RANDOM));
    }

    protected AddItemRequest buildAddItemRequest(@lombok.NonNull SubscriptionItem item,
            @lombok.NonNull Subscription subscription) {
        AddItemRequest addItemRequest = typeFactory.get(AddItemRequest.class);
        addItemRequest.setProductId(item.getItemRef());
        addItemRequest.setQuantity(item.getQuantity());
        addItemRequest.setItemChoiceKey(item.getAddOnKey());
        addItemRequest.getItemAttributes().put(SUBSCRIPTION_ITEM_ID, item.getId());
        addItemRequest.setTermDurationType(subscription.getTermDurationType());
        addItemRequest.setTermDurationLength(subscription.getTermDurationLength());

        if (item.getParentItemRef() == null) {
            addItemRequest.getCartAttributes().put(SUBSCRIPTION_ID, subscription.getId());
            addItemRequest.getCartAttributes().put(FLOW,
                    DefaultSubscriptionActionFlow.EDIT.name());
        }

        return addItemRequest;
    }
}
