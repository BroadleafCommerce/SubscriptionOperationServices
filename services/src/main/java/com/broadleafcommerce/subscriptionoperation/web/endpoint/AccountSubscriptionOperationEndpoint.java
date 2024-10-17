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


import org.broadleafcommerce.frameworkmapping.annotation.FrameworkGetMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkRestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.context.ContextOperation;
import com.broadleafcommerce.data.tracking.core.policy.IdentityType;
import com.broadleafcommerce.data.tracking.core.policy.Policy;
import com.broadleafcommerce.data.tracking.core.type.OperationType;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.broadleafcommerce.subscriptionoperation.service.SubscriptionOperationService;

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
            @RequestParam(value = "getActions", required = false,
                    defaultValue = "false") boolean getActions,
            @PageableDefault(sort = "tracking.basicAudit.creationTime",
                    direction = Sort.Direction.DESC) Pageable page,
            Node filters,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        return subscriptionOperationService.readSubscriptionsForUserRefTypeAndUserRef(
                DefaultUserRefTypes.BLC_ACCOUNT.name(), accountId, getActions, page, filters,
                contextInfo);
    }

    @FrameworkGetMapping(value = "/{subscriptionId}")
    @Policy(permissionRoots = "ACCOUNT_SUBSCRIPTION",
            identityTypes = {IdentityType.ADMIN, IdentityType.OWNER},
            ownerIdentifierParam = 0, ownerIdentifier = "acct_id,parent_accts")
    public SubscriptionWithItems readAccountSubscription(
            @PathVariable("accountId") String accountId,
            @PathVariable("subscriptionId") String subscriptionId,
            @RequestParam(value = "getActions", required = false,
                    defaultValue = "false") boolean getActions,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        return subscriptionOperationService.readUserSubscriptionById(
                DefaultUserRefTypes.BLC_ACCOUNT.name(), accountId, subscriptionId, getActions,
                contextInfo);
    }
}
