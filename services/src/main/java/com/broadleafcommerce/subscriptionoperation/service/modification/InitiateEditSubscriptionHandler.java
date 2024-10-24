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

    public static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Getter(value = AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    @Getter(value = AccessLevel.PROTECTED)
    private final CartOperationProvider cartOperationProvider;

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
        createCartRequest.setName(subscription.getName() + " - " + ULID.random(SECURE_RANDOM));
        createCartRequest.setType(DefaultCartTypes.STANDARD.name());
        PriceCartRequest priceCartRequest = typeFactory.get(PriceCartRequest.class);
        priceCartRequest.setCurrency(subscription.getCurrency());
        createCartRequest.setPriceCartRequest(priceCartRequest);

        Map<String, List<String>> subItemIdsToChildrenIds = new HashMap<>();
        Map<String, AddItemRequest> addItemRequestsBySubItemId = new HashMap<>();
        AtomicReference<String> rootItemId = new AtomicReference<>();
        for (SubscriptionItem next : items) {
            List<String> children = items.stream()
                    .filter(item -> next.getItemRef().equals(item.getParentItemRef()))
                    .map(SubscriptionItem::getId)
                    .collect(Collectors.toList());
            subItemIdsToChildrenIds.put(next.getId(), children);
            addItemRequestsBySubItemId.put(next.getId(), buildAddItemRequest(next));

            if (next.getParentItemRef() == null) {
                rootItemId.set(next.getId());
            }
        }

        // add children as dependents
        subItemIdsToChildrenIds.forEach((id, children) -> {
            AddItemRequest parent = addItemRequestsBySubItemId.get(id);
            children.stream()
                    .map(addItemRequestsBySubItemId::get)
                    .forEach(addItemRequest -> parent.getDependentCartItems().add(addItemRequest));
        });

        AddItemRequest rootItem = addItemRequestsBySubItemId.get(rootItemId.get());
        rootItem.getCartAttributes().put("subscriptionId", subscription.getId());
        rootItem.getCartAttributes().put("flow", DefaultSubscriptionActionFlow.EDIT.name());
        rootItem.setTermDurationType(subscription.getTermDurationType());
        rootItem.setTermDurationLength(subscription.getTermDurationLength());
        createCartRequest.getAddItemRequests().add(rootItem);

        return createCartRequest;
    }

    protected AddItemRequest buildAddItemRequest(@lombok.NonNull SubscriptionItem item) {
        AddItemRequest addItemRequest = typeFactory.get(AddItemRequest.class);
        addItemRequest.setProductId(item.getItemRef());
        addItemRequest.setQuantity(item.getQuantity());
        addItemRequest.setItemChoiceKey(item.getAddOnKey());
        addItemRequest.getItemAttributes().put("subscriptionItemId", item.getId());
        addItemRequest.getItemAttributes().put("parentRef", item.getParentItemRef());
        addItemRequest.getItemAttributes().put("parentRefType", item.getParentItemRefType());

        return addItemRequest;
    }
}
