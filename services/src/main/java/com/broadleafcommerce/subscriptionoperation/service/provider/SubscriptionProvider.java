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
package com.broadleafcommerce.subscriptionoperation.service.provider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionUpdateDTO;

import cz.jirutka.rsql.parser.ast.Node;


/**
 * Provider interfacing with Broadleaf's BillingServices to gather, update, or create
 * {@link Subscription} and related entities.
 */
public interface SubscriptionProvider<SWI extends SubscriptionWithItems> {

    /**
     * Persists a subscription alongside its items
     *
     * @param subscriptionWithItems subscription with items to persist
     * @param contextInfo context information around multi-tenant state
     * @return the created subscription and items
     */
    SWI create(SWI subscriptionWithItems, @Nullable ContextInfo contextInfo);

    /**
     * Retrieves a page of subscriptions with items for a given user type and user id, taking into
     * account the provided filters and paging
     *
     * @param userRefType type of the owning user
     * @param userRef id of the owning user
     * @param page pageable
     * @param filters Additional RSQL filters
     * @param contextInfo context information around multi-tenant state
     * @return a page of subscriptions with items for a given user type and user id
     */
    Page<SWI> readSubscriptionsForUserRefTypeAndUserRef(String userRefType,
            String userRef,
            @Nullable Pageable page,
            @Nullable Node filters,
            @Nullable ContextInfo contextInfo);

    /**
     * Retrieves a subscription with items for a given id
     *
     * @param subscriptionId The id of the subscription
     * @param contextInfo context information around multi-tenant state
     * @return a subscription with items for the given id
     */
    SWI readSubscriptionById(String subscriptionId, @Nullable ContextInfo contextInfo);

    /**
     * Retrieves a user-owned subscription with items for a given id
     *
     * @param userRefType type of the owning user
     * @param userRef id of the owning user
     * @param subscriptionId The id of the subscription
     * @param contextInfo context information around multi-tenant state
     * @return a user-owned subscription with items for a given id
     */
    SWI readUserSubscriptionById(String userRefType,
            String userRef,
            String subscriptionId,
            @Nullable ContextInfo contextInfo);

    /**
     * Patches a subscription.
     *
     * @param subscriptionUpdateDTO the {@link SubscriptionUpdateDTO} used for the patch operation
     * @param contextInfo context information around multi-tenant state
     * @return the updated subscription
     */
    Subscription patch(String subscriptionId,
            SubscriptionUpdateDTO subscriptionUpdateDTO,
            @Nullable ContextInfo contextInfo);
}
