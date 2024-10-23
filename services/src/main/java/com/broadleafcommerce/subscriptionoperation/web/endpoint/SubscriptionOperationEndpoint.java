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
import com.broadleafcommerce.data.tracking.core.policy.Policy;
import com.broadleafcommerce.data.tracking.core.type.OperationType;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.broadleafcommerce.subscriptionoperation.service.SubscriptionOperationService;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.ModifySubscriptionResponse;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;

import cz.jirutka.rsql.parser.ast.Node;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@FrameworkRestController
@FrameworkMapping(SubscriptionOperationEndpoint.BASE_URI)
public class SubscriptionOperationEndpoint {
    public static final String BASE_URI = "/subscription-ops";

    @Getter(AccessLevel.PROTECTED)
    protected final SubscriptionOperationService<SubscriptionWithItems> subscriptionOperationService;

    @FrameworkGetMapping(params = {"userRefType", "userRef"})
    @Policy(permissionRoots = "SYSTEM_SUBSCRIPTION")
    public Page<SubscriptionWithItems> readAllUserOwnedSubscriptions(
            @RequestParam("userRefType") String userRefType,
            @RequestParam("userRef") String userRef,
            @RequestParam(value = "getActions", required = false,
                    defaultValue = "false") boolean getActions,
            @PageableDefault(sort = "tracking.basicAudit.creationTime",
                    direction = Sort.Direction.DESC) Pageable page,
            Node filters,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        return subscriptionOperationService.readSubscriptionsForUserRefTypeAndUserRef(userRefType,
                userRef, getActions, page, filters, contextInfo);
    }

    @FrameworkGetMapping(value = "/{subscriptionId}")
    @Policy(permissionRoots = "SYSTEM_SUBSCRIPTION")
    public SubscriptionWithItems readSubscriptionById(
            @PathVariable("subscriptionId") String subscriptionId,
            @ContextOperation(OperationType.READ) final ContextInfo contextInfo) {
        return subscriptionOperationService.readSubscriptionById(subscriptionId, contextInfo);
    }

    @FrameworkPostMapping
    @Policy(permissionRoots = "SYSTEM_SUBSCRIPTION", operationTypes = OperationType.CREATE)
    public SubscriptionWithItems createSubscription(
            @RequestBody SubscriptionCreationRequest subscriptionCreationRequest,
            @ContextOperation(OperationType.CREATE) final ContextInfo contextInfo) {
        return subscriptionOperationService.createSubscriptionWithItems(subscriptionCreationRequest,
                contextInfo);
    }

    @FrameworkPostMapping(value = "/{subscriptionId}/modifications",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Policy(permissionRoots = {"SYSTEM_SUBSCRIPTION"}, operationTypes = OperationType.UPDATE)
    public ModifySubscriptionResponse modifySubscription(
            @PathVariable("subscriptionId") String subscriptionId,
            @RequestBody ModifySubscriptionRequest request,
            @ContextOperation(OperationType.UPDATE) final ContextInfo contextInfo) {
        request.setSubscriptionId(subscriptionId);
        return subscriptionOperationService.modifySubscription(request, contextInfo);
    }
}
