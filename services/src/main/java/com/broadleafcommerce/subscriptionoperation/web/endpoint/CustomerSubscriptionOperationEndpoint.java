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
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkPostMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkRestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionActionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionActionResponse;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionUpgradeRequest;

import cz.jirutka.rsql.parser.ast.Node;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@FrameworkRestController
@FrameworkMapping(CustomerSubscriptionOperationEndpoint.BASE_URI)
public class CustomerSubscriptionOperationEndpoint {

    public static final String BASE_URI = "/customers/{customerId}/subscriptions";

    @Getter(AccessLevel.PROTECTED)
    protected final SubscriptionOperationService<Subscription, SubscriptionItem, SubscriptionWithItems> subscriptionOperationService;

    @FrameworkGetMapping
    @Policy(permissionRoots = "CUSTOMER_SUBSCRIPTION",
            identityTypes = {IdentityType.OWNER},
            ownerIdentifierParam = 0)
    public Page<SubscriptionWithItems> readAllCustomerSubscriptions(
            @PathVariable("customerId") String customerId,
            @RequestParam(value = "getActions", required = false,
                    defaultValue = "false") boolean getActions,
            @PageableDefault(sort = "tracking.basicAudit.creationTime",
                    direction = Sort.Direction.DESC) Pageable page,
            Node filters,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        return subscriptionOperationService.readSubscriptionsForUserRefTypeAndUserRef(
                DefaultUserRefTypes.BLC_CUSTOMER.name(), customerId, getActions, page, filters, contextInfo);
    }

    @FrameworkGetMapping(value = "/{subscriptionId}")
    @Policy(permissionRoots = "CUSTOMER_SUBSCRIPTION",
            identityTypes = {IdentityType.OWNER},
            ownerIdentifierParam = 0)
    public SubscriptionWithItems readCustomerSubscription(
            @PathVariable("customerId") String customerId,
            @PathVariable("subscriptionId") String subscriptionId,
            @RequestParam(value = "getActions", required = false,
                    defaultValue = "false") boolean getActions,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        return subscriptionOperationService.readUserSubscriptionById(
                DefaultUserRefTypes.BLC_CUSTOMER.name(), customerId, subscriptionId, contextInfo);
    }

    @FrameworkPostMapping(value = "/{subscriptionId}/actions")
    @Policy(permissionRoots = "CUSTOMER_SUBSCRIPTION",
            identityTypes = {IdentityType.OWNER},
            ownerIdentifierParam = 0)
    public SubscriptionActionResponse readCustomerSubscriptionActions(
            @PathVariable("customerId") String customerId,
            @PathVariable("subscriptionId") String subscriptionId,
            @RequestBody SubscriptionActionRequest request,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        request.setSubscriptionId(subscriptionId);
        request.setUserType(DefaultUserRefTypes.BLC_CUSTOMER.name());
        request.setUserId(customerId);

        return subscriptionOperationService.readSubscriptionActions(request, contextInfo);
    }

    @FrameworkPostMapping(value = "/{subscriptionId}/upgrade",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Policy(permissionRoots = {"CUSTOMER_SUBSCRIPTION"},
            identityTypes = {IdentityType.OWNER},
            ownerIdentifierParam = 0,
            operationTypes = OperationType.UPDATE)
    public Subscription upgradeSubscription(
            @PathVariable("customerId") String customerId,
            @PathVariable("subscriptionId") String subscriptionId,
            @RequestBody SubscriptionUpgradeRequest upgradeRequest,
            @ContextOperation(OperationType.UPDATE) final ContextInfo contextInfo) {
        upgradeRequest.setPriorSubscriptionId(subscriptionId);
        return subscriptionOperationService.upgradeSubscription(upgradeRequest, contextInfo);
    }

    @FrameworkPostMapping(value = "/{subscriptionId}/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Policy(permissionRoots = {"CUSTOMER_SUBSCRIPTION"},
            identityTypes = {IdentityType.OWNER},
            ownerIdentifierParam = 0,
            operationTypes = OperationType.UPDATE)
    public Subscription cancelSubscription(
            @PathVariable("customerId") String customerId,
            @PathVariable("subscriptionId") String subscriptionId,
            @RequestBody SubscriptionCancellationRequest subscriptionCancellationRequest,
            @ContextOperation(OperationType.UPDATE) final ContextInfo contextInfo) {
        subscriptionCancellationRequest.setSubscriptionId(subscriptionId);
        return subscriptionOperationService.cancelSubscription(subscriptionCancellationRequest,
                contextInfo);
    }
}
