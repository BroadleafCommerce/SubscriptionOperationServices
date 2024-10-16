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
package com.broadleafcommerce.subscriptionoperation.web.endpoint;

import static com.broadleafcommerce.data.tracking.core.filtering.fetch.rsql.RsqlSearchOperation.EQUAL;

import org.apache.commons.collections4.CollectionUtils;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkGetMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkRestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.context.ContextOperation;
import com.broadleafcommerce.data.tracking.core.exception.EntityMissingException;
import com.broadleafcommerce.data.tracking.core.policy.IdentityType;
import com.broadleafcommerce.data.tracking.core.policy.Policy;
import com.broadleafcommerce.data.tracking.core.type.OperationType;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.broadleafcommerce.subscriptionoperation.service.SubscriptionOperationService;

import java.util.List;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.Node;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@FrameworkRestController
@FrameworkMapping(AccountSubscriptionOperationEndpoint.BASE_URI)
public class AccountSubscriptionOperationEndpoint {

    public static final String BASE_URI = "/accounts/{accountId}/subscriptions";

    @Getter(AccessLevel.PROTECTED)
    protected final SubscriptionOperationService<Subscription, SubscriptionItem, SubscriptionWithItems> subscriptionOperationService;

    @FrameworkGetMapping
    @Policy(permissionRoots = "ACCOUNT_SUBSCRIPTION",
            identityTypes = {IdentityType.ADMIN, IdentityType.OWNER},
            ownerIdentifierParam = 0, ownerIdentifier = "acct_id,parent_accts")
    public Page<SubscriptionWithItems> readAllAccountSubscriptions(
            @PathVariable("accountId") String accountId,
            @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Pageable page,
            Node filters,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        return subscriptionOperationService.readSubscriptionsForUserTypeAndUserId(
                DefaultUserRefTypes.BLC_ACCOUNT.name(), accountId, page, filters, contextInfo);
    }

    @FrameworkGetMapping(value = "/{subscriptionId}")
    @Policy(permissionRoots = "ACCOUNT_SUBSCRIPTION",
            identityTypes = {IdentityType.ADMIN, IdentityType.OWNER},
            ownerIdentifierParam = 0, ownerIdentifier = "acct_id,parent_accts")
    public SubscriptionWithItems readAccountSubscription(
            @PathVariable("accountId") String accountId,
            @PathVariable("subscriptionId") String subscriptionId,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        Node subscriptionIdFilter = buildSubscriptionIdFilter(subscriptionId, contextInfo);

        List<SubscriptionWithItems> subscriptions = subscriptionOperationService
                .readSubscriptionsForUserTypeAndUserId(
                        DefaultUserRefTypes.BLC_ACCOUNT.name(), accountId, Pageable.unpaged(),
                        subscriptionIdFilter, contextInfo)
                .getContent();

        if (CollectionUtils.isEmpty(subscriptions)) {
            throw new EntityMissingException();
        } else if (subscriptions.size() > 1) {
            log.warn(
                    "There is more than 1 subscription with the same id for the account. Account ID: {} | Subscription ID: {}",
                    accountId, subscriptionId);
        }

        return subscriptions.get(0);
    }

    protected Node buildSubscriptionIdFilter(@lombok.NonNull String subscriptionId,
            @Nullable ContextInfo contextInfo) {
        return new ComparisonNode(EQUAL.getOperator(),
                "id",
                List.of(subscriptionId));
    }
}
