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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.filtering.fetch.rsql.EmptyNode;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionUpgradeRequest;

import cz.jirutka.rsql.parser.ast.Node;

/**
 * Service for operations on subscriptions and their items
 */
public interface SubscriptionOperationService<S extends Subscription, I extends SubscriptionItem, SWI extends SubscriptionWithItems> {

    /**
     * This method reads subscriptions for a given user type and user id, additionally filtered and
     * paginated by given parameters
     *
     * @param userRefType user type, see {@link DefaultUserRefTypes}
     * @param userRef id of owning user or account
     * @param page information about which page of results to return from the database.
     * @param filters additional filters to apply in the query. Should be {@link EmptyNode} if no
     *        additional filters should be applied.
     * @param contextInfo context information around multi-tenant state
     * @return Subscriptions with items matching the given criteria
     */
    Page<SWI> readSubscriptionsForUserRefTypeAndUserRef(String userRefType,
            String userRef,
            @Nullable Pageable page,
            @Nullable Node filters,
            @Nullable ContextInfo contextInfo);

    /**
     * This method attempts to find a {@link SubscriptionWithItems} by id which is owned by the
     * user/account
     *
     * @param userRefType user type, see {@link DefaultUserRefTypes}
     * @param userRef id of owning user or account
     * @param subscriptionId The id of the {@link Subscription} that is intended to be gathered
     * @param contextInfo context information around multi-tenant state
     * @return Subscriptions with items matching the given criteria
     */
    SWI readUserSubscriptionById(String userRefType,
            String userRef,
            String subscriptionId,
            @Nullable ContextInfo contextInfo);

    /**
     * This method reads a subscription for the given id
     *
     * @param subscriptionId The id of the {@link Subscription} that is intended to be gathered
     * @param contextInfo context information around multi-tenant state
     * @return a subscription with items identified by the subscription id
     */
    SWI readSubscriptionById(String subscriptionId, @Nullable ContextInfo contextInfo);

    /**
     * Builds out a {@link SubscriptionWithItems} and calls a provider to persist them in the
     * resource-tier service
     *
     * @param subscriptionCreationRequest request DTO with necessary fields to build a subscription
     * @param contextInfo context information around multi-tenant state
     * @return a created subscription with its items
     */
    SWI createSubscriptionWithItems(SubscriptionCreationRequest subscriptionCreationRequest,
            @Nullable ContextInfo contextInfo);

    /**
     * TODO
     *
     * @param subscriptionCancellationRequest
     * @param context
     * @return
     */
    S cancelSubscription(SubscriptionCancellationRequest subscriptionCancellationRequest,
            @Nullable ContextInfo context);

    /**
     * TODO
     *
     * @param upgradeRequest
     * @param contextInfo
     * @return
     */
    S upgradeSubscription(SubscriptionUpgradeRequest upgradeRequest,
            @Nullable ContextInfo contextInfo);
}
