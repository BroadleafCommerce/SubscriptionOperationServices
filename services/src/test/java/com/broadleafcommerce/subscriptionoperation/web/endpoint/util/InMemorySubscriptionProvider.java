/*
 * Copyright (C) 2021 Broadleaf Commerce
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
package com.broadleafcommerce.subscriptionoperation.web.endpoint.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.exception.EntityMissingException;
import com.broadleafcommerce.data.tracking.core.filtering.business.domain.ContextState;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import cz.jirutka.rsql.parser.ast.Node;
import io.azam.ulidj.ULID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * In-memory implementation of {@link SubscriptionProvider} for use in integration tests.
 *
 * @author Nick Crum (ncrum)
 */
@RequiredArgsConstructor
public class InMemorySubscriptionProvider implements SubscriptionProvider<SubscriptionWithItems> {

    private final ObjectMapper objectMapper;

    @Getter
    private final Map<String, SubscriptionWithItems> store = new LinkedHashMap<>();

    public void clearStore() {
        store.clear();
    }

    public SubscriptionWithItems persist(String id, SubscriptionWithItems subscriptionWithItems) {
        subscriptionWithItems = simulateSerialization(subscriptionWithItems);
        store.put(id, subscriptionWithItems);
        return subscriptionWithItems;
    }

    /**
     * We use this method to intentionally serialize and deserialize to simulate how data would be
     * stored within an actual application where data is being serialized and deserialized to and
     * from HTTP requests. This helps ensure we don't end up with behavior that is dependent on
     * mutations and side effects on objects that should have been serialized.
     */
    public SubscriptionWithItems simulateSerialization(
            SubscriptionWithItems subscriptionWithItems) {
        try {
            String serialized = objectMapper.writeValueAsString(subscriptionWithItems);
            return objectMapper.readValue(serialized, SubscriptionWithItems.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public SubscriptionWithItems create(SubscriptionWithItems subscriptionWithItems,
            ContextInfo contextInfo) {
        Subscription subscription = subscriptionWithItems.getSubscription();
        if (subscription.getId() == null) {
            subscription.setId(ULID.random());
        }
        setContextStateForSubscription(subscriptionWithItems, contextInfo);
        return persist(subscription.getId(), subscriptionWithItems);
    }

    @Override
    public Page<SubscriptionWithItems> readSubscriptionsForUserRefTypeAndUserRef(String userRefType,
            String userRef,
            Pageable page,
            Node filters,
            ContextInfo contextInfo) {
        List<SubscriptionWithItems> subscriptionWithItemsList = getStore().values().stream()
                .filter(userMatches(userRefType, userRef))
                .filter(contextMatches(contextInfo))
                .map(this::simulateSerialization)
                .toList();

        if (page == null) {
            page = Pageable.unpaged();
        }

        return new PageImpl<>(subscriptionWithItemsList, page, subscriptionWithItemsList.size());
    }

    @Override
    public SubscriptionWithItems readSubscriptionById(String subscriptionId,
            @Nullable ContextInfo contextInfo) {
        return getStore().values().stream()
                .filter(subscriptionWithItems -> Objects
                        .equals(subscriptionWithItems.getSubscription().getId(), subscriptionId))
                .filter(contextMatches(contextInfo))
                .map(this::simulateSerialization)
                .findFirst()
                .orElseThrow(EntityMissingException::new);
    }

    @Override
    public SubscriptionWithItems readUserSubscriptionById(String userRefType,
            String userRef,
            String subscriptionId,
            @Nullable ContextInfo contextInfo) {
        return getStore().values().stream()
                .filter(userMatches(userRefType, userRef))
                .filter(subscriptionWithItems -> Objects
                        .equals(subscriptionWithItems.getSubscription().getId(), subscriptionId))
                .filter(contextMatches(contextInfo))
                .map(this::simulateSerialization)
                .findFirst()
                .orElseThrow(EntityMissingException::new);
    }

    protected Predicate<SubscriptionWithItems> userMatches(String userRefType,
            @Nullable String userRef) {
        return sWI -> sWI.getSubscription().getUserRef() != null
                && Objects.equals(sWI.getSubscription().getUserRefType(), userRefType)
                && Objects.equals(sWI.getSubscription().getUserRef(), userRef);
    }

    protected Predicate<SubscriptionWithItems> contextMatches(@Nullable ContextInfo contextInfo) {
        return sWI -> contextInfo == null
                || tenantMatches(sWI, contextInfo.getContextRequest().getApplicationId());
    }

    protected boolean tenantMatches(SubscriptionWithItems subscriptionWithItems,
            @Nullable String tenantId) {
        return StringUtils.isBlank(tenantId) || StringUtils
                .equals(subscriptionWithItems.getSubscription().getContextState().getTenant(),
                        tenantId);
    }

    private void setContextStateForSubscription(SubscriptionWithItems subscriptionWithItems,
            @Nullable ContextInfo contextInfo) {
        Subscription subscription = subscriptionWithItems.getSubscription();
        subscription.setContextState(new ContextState());
        if (contextInfo == null) {
            return;
        }
        subscription.getContextState().setTenant(contextInfo.getContextRequest().getTenantId());
        for (SubscriptionItem item : subscriptionWithItems.getSubscriptionItems()) {
            item.getContextState().setTenant(contextInfo.getContextRequest().getTenantId());
        }
    }


}
