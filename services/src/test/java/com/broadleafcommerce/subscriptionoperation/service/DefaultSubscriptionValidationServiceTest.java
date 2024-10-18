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

import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType.CANCEL;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType.DOWNGRADE;
import static com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionType.UPGRADE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.broadleafcommerce.data.tracking.core.policy.PolicyResponse;
import com.broadleafcommerce.data.tracking.core.policy.trackable.TrackablePolicyUtils;
import com.broadleafcommerce.subscriptionoperation.domain.Product;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.broadleafcommerce.subscriptionoperation.service.exception.InsufficientSubscriptionAccessException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionCreationRequestException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionDowngradeRequestException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionUpgradeRequestException;
import com.broadleafcommerce.subscriptionoperation.service.provider.CatalogProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCancellationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionDowngradeRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionItemCreationRequest;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionUpgradeRequest;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class DefaultSubscriptionValidationServiceTest {

    private static final String CUSTOMER_ID = "customerId";

    @Mock
    private CatalogProvider<Product> catalogProvider;
    @Mock
    private TrackablePolicyUtils policyUtils;

    private DefaultSubscriptionValidationService subValidationService;

    @BeforeEach
    void setup() {
        subValidationService = spy(new DefaultSubscriptionValidationService(catalogProvider));
        subValidationService.setPolicyUtils(policyUtils);
    }

    @Test
    public void testValidSubCreation() {
        SubscriptionCreationRequest request = buildValidCreationRequest();
        assertDoesNotThrow(() -> subValidationService.validateSubscriptionCreation(request, null));
    }

    @Test
    public void testInvalidSubCreation_missingUserRef() {
        SubscriptionCreationRequest request = buildValidCreationRequest();
        request.setUserRefType(null);

        assertThatThrownBy(() -> subValidationService.validateSubscriptionCreation(request, null))
                .isInstanceOf(InvalidSubscriptionCreationRequestException.class)
                .hasMessage(
                        "A subscription must be given an owning user/account via userRefType and userRef.");

        request.setUserRefType("refType");
        request.setUserRef(null);
        assertThatThrownBy(() -> subValidationService.validateSubscriptionCreation(request, null))
                .isInstanceOf(InvalidSubscriptionCreationRequestException.class)
                .hasMessage(
                        "A subscription must be given an owning user/account via userRefType and userRef.");
    }

    @Test
    public void testInvalidSubCreation_missingPeriodType() {
        SubscriptionCreationRequest request = buildValidCreationRequest();
        request.setPeriodType(null);
        assertThatThrownBy(() -> subValidationService.validateSubscriptionCreation(request, null))
                .isInstanceOf(InvalidSubscriptionCreationRequestException.class)
                .hasMessage("A subscription must be given a periodType or billingFrequency.");
    }

    @Test
    public void testInvalidSubCreation_missingSource() {
        SubscriptionCreationRequest request = buildValidCreationRequest();
        request.setSubscriptionSource(null);
        request.setSubscriptionSourceRef(null);

        assertThatThrownBy(() -> subValidationService.validateSubscriptionCreation(request, null))
                .isInstanceOf(InvalidSubscriptionCreationRequestException.class)
                .hasMessage("A subscription must be given a source or sourceRef.");
    }

    @Test
    public void testInvalidSubCreation_missingItemCreationRequests() {
        SubscriptionCreationRequest request = buildValidCreationRequest();
        request.setItemCreationRequests(Collections.emptyList());

        assertThatThrownBy(() -> subValidationService.validateSubscriptionCreation(request, null))
                .isInstanceOf(InvalidSubscriptionCreationRequestException.class)
                .hasMessage("Subscription items must also be defined for the subscription.");
    }

    @Test
    public void testValidateSubscriptionCancellation() {
        String subId = "subscriptionId";
        SubscriptionCancellationRequest request = new SubscriptionCancellationRequest();
        request.setSubscriptionId(subId);

        when(policyUtils.validatePermissions(any(), any()))
                .thenReturn(PolicyResponse.VALID);

        assertDoesNotThrow(
                () -> subValidationService.validateSubscriptionCancellation(request, null));

        assertCommonValidationCalled(subId, CANCEL.name());
    }

    @Test
    public void testValidateSubscriptionCancellation_invalidAccess() {
        String subId = "subscriptionId";
        SubscriptionCancellationRequest request = new SubscriptionCancellationRequest();
        request.setSubscriptionId(subId);

        when(policyUtils.validatePermissions(any(), any()))
                .thenReturn(PolicyResponse.NOT_PERMITTED);

        assertThatThrownBy(
                () -> subValidationService.validateSubscriptionCancellation(request, null))
                        .isInstanceOf(InsufficientSubscriptionAccessException.class);
    }

    @Test
    public void testValidateSubscriptionUpgrade() {
        String subId = "subscriptionId";
        SubscriptionUpgradeRequest request = new SubscriptionUpgradeRequest();
        request.setPriorSubscriptionId(subId);

        Product subscriptionProduct = new Product();
        subscriptionProduct.setUpgradeProductId("someUpgradeProductId");

        when(policyUtils.validatePermissions(any(), any()))
                .thenReturn(PolicyResponse.VALID);
        when(catalogProvider.readProductById(anyString(), any()))
                .thenReturn(subscriptionProduct);

        assertDoesNotThrow(() -> subValidationService.validateSubscriptionUpgrade(request, null));

        assertCommonValidationCalled(subId, UPGRADE.name());
    }

    @Test
    public void testValidateSubscriptionUpgrade_invalidAccess() {
        String subId = "subscriptionId";
        SubscriptionUpgradeRequest request = new SubscriptionUpgradeRequest();
        request.setPriorSubscriptionId(subId);

        Product subscriptionProduct = new Product();
        subscriptionProduct.setUpgradeProductId("someUpgradeProductId");

        when(policyUtils.validatePermissions(any(), any()))
                .thenReturn(PolicyResponse.NOT_PERMITTED);

        assertThatThrownBy(() -> subValidationService.validateSubscriptionUpgrade(request, null))
                .isInstanceOf(InsufficientSubscriptionAccessException.class);
    }

    // TODO: Update tests once upgrade flow is implemented
    @Test
    public void testValidateSubscriptionUpgrade_ineligibleProduct() {
        String subId = "subscriptionId";
        SubscriptionUpgradeRequest request = new SubscriptionUpgradeRequest();
        request.setPriorSubscriptionId(subId);

        Product subscriptionProduct = new Product();
        subscriptionProduct.setUpgradeProductId(null);

        when(policyUtils.validatePermissions(any(), any()))
                .thenReturn(PolicyResponse.VALID);
        when(catalogProvider.readProductById(anyString(), any()))
                .thenReturn(subscriptionProduct);

        assertThatThrownBy(() -> subValidationService.validateSubscriptionUpgrade(request, null))
                .isInstanceOf(InvalidSubscriptionUpgradeRequestException.class);
    }

    @Test
    public void testValidateSubscriptionDowngrade() {
        String subId = "subscriptionId";
        SubscriptionDowngradeRequest request = new SubscriptionDowngradeRequest();
        request.setPriorSubscriptionId(subId);

        Product subscriptionProduct = new Product();
        subscriptionProduct.setDowngradeProductId("someDowngradeProductId");

        when(policyUtils.validatePermissions(any(), any()))
                .thenReturn(PolicyResponse.VALID);
        when(catalogProvider.readProductById(anyString(), any()))
                .thenReturn(subscriptionProduct);

        assertDoesNotThrow(() -> subValidationService.validateSubscriptionDowngrade(request, null));

        assertCommonValidationCalled(subId, DOWNGRADE.name());
    }

    @Test
    public void testValidateSubscriptionDowngrade_invalidAccess() {
        String subId = "subscriptionId";
        SubscriptionDowngradeRequest request = new SubscriptionDowngradeRequest();
        request.setPriorSubscriptionId(subId);

        Product subscriptionProduct = new Product();
        subscriptionProduct.setDowngradeProductId("someDowngradeProductId");

        when(policyUtils.validatePermissions(any(), any()))
                .thenReturn(PolicyResponse.NOT_PERMITTED);

        assertThatThrownBy(() -> subValidationService.validateSubscriptionDowngrade(request, null))
                .isInstanceOf(InsufficientSubscriptionAccessException.class);
    }

    // TODO: Update tests once upgrade flow is implemented
    @Test
    public void testValidateSubscriptionDowngrade_ineligibleProduct() {
        String subId = "subscriptionId";
        SubscriptionDowngradeRequest request = new SubscriptionDowngradeRequest();
        request.setPriorSubscriptionId(subId);

        Product subscriptionProduct = new Product();
        subscriptionProduct.setDowngradeProductId(null);

        when(policyUtils.validatePermissions(any(), any()))
                .thenReturn(PolicyResponse.VALID);
        when(catalogProvider.readProductById(anyString(), any()))
                .thenReturn(subscriptionProduct);

        assertThatThrownBy(() -> subValidationService.validateSubscriptionDowngrade(request, null))
                .isInstanceOf(InvalidSubscriptionDowngradeRequestException.class);
    }

    private void assertCommonValidationCalled(String subscriptionId, String actionType) {
        verify(subValidationService, times(1)).getRequiredPermissions(eq(actionType));
        verify(policyUtils, times(1)).validatePermissions(any(), any());
        verify(subValidationService, times(1)).validateAdditionalPermissionRules(eq(subscriptionId),
                eq(actionType), any());
        verify(subValidationService, times(1)).validateBusinessRules(eq(subscriptionId),
                eq(actionType), any());
    }

    private SubscriptionCreationRequest buildValidCreationRequest() {
        SubscriptionCreationRequest request = new SubscriptionCreationRequest();
        request.setUserRefType(DefaultUserRefTypes.BLC_CUSTOMER.name());
        request.setUserRef(CUSTOMER_ID);
        request.setPeriodType("MONTHLY");
        request.setSubscriptionSource("SOME_SOURCE");
        request.setSubscriptionSourceRef("someSourceId");

        SubscriptionItemCreationRequest itemRequest = new SubscriptionItemCreationRequest();
        itemRequest.setItemName("name");
        itemRequest.setItemRef("ref");
        itemRequest.setItemRefType("refType");
        itemRequest.setQuantity(1);

        request.setItemCreationRequests(Collections.singletonList(itemRequest));

        return request;
    }
}
