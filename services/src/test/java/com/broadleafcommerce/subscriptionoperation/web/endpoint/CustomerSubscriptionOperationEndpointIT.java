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

import static com.broadleafcommerce.data.tracking.core.context.ContextInfoHandlerMethodArgumentResolver.CONTEXT_REQUEST_HEADER;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.context.ContextRequest;
import com.broadleafcommerce.data.tracking.core.type.OperationType;
import com.broadleafcommerce.oauth2.resource.security.test.MockMvcOAuth2AuthenticationUtil;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserTypes;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.web.endpoint.util.InMemorySubscriptionProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import io.azam.ulidj.ULID;

@WithMockUser
@SpringBootTest
@AutoConfigureMockMvc
class CustomerSubscriptionOperationEndpointIT {

    protected static final String SYSTEM_SUBSCRIPTION_URI = "/subscription-ops";
    protected static final String CUSTOMER_URI = "/customer";
    protected static final String CUSTOMER_READ_URI = "/{customerId}/subscriptions";

    protected static final String CUSTOMER_SUBSCRIPTION = "CUSTOMER_SUBSCRIPTION";

    protected static final String TENANT = "tenant1";
    protected static final String REGISTERED_CUSTOMER = "registeredCustomer";
    protected static final String REGISTERED_CUSTOMER_ID = "1";
    protected static final String ACCOUNT_ID = "accountId";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MockMvcOAuth2AuthenticationUtil authUtil;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    InMemorySubscriptionProvider inMemorySubscriptionProvider;

    @Configuration
    @EnableAutoConfiguration
    static class Config {
        @Bean
        public SubscriptionProvider<SubscriptionWithItems> cartProvider(ObjectMapper objectMapper) {
            return spy(new InMemorySubscriptionProvider(objectMapper));
        }
    }

    @BeforeEach
    void beforeEach() {
        inMemorySubscriptionProvider.clearStore();
        reset(inMemorySubscriptionProvider);
    }

    @Test
    void cannotReadSubscriptionsWithoutPermissionAndWrongUser() throws Exception {
        inMemorySubscriptionProvider.create(getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID),
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", "2");

        mockMvc.perform(get(CUSTOMER_URI + CUSTOMER_READ_URI, "2")
                .with(authUtil.withAuthoritiesAndDetails(Sets.newSet("RANDOM_PERMISSION"),
                        authDetails))
                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSubscriptionsWithoutPermissionWithCorrectUser() throws Exception {
        inMemorySubscriptionProvider.create(getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID),
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", REGISTERED_CUSTOMER_ID);

        mockMvc.perform(get(CUSTOMER_URI + CUSTOMER_READ_URI, REGISTERED_CUSTOMER_ID)
                .with(authUtil.withAuthoritiesAndDetails(Sets.newSet("RANDOM_PERMISSION"),
                        authDetails))
                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSubscriptionsWithPermissionWithWrongUser() throws Exception {
        inMemorySubscriptionProvider.create(getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID),
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", "2");

        mockMvc.perform(get(CUSTOMER_URI + CUSTOMER_READ_URI, REGISTERED_CUSTOMER_ID)
                .with(authUtil.withAuthoritiesAndDetails(Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                        authDetails))
                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void readsSubscriptionsWithPermission() throws Exception {
        inMemorySubscriptionProvider.create(getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID),
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", REGISTERED_CUSTOMER_ID);

        mockMvc.perform(get(CUSTOMER_URI + CUSTOMER_READ_URI, REGISTERED_CUSTOMER_ID)
                .with(authUtil.withAuthoritiesAndDetails(Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                        authDetails))
                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].subscription.userRefType")
                                .value(DefaultUserTypes.BLC_CUSTOMER.name()))
                .andExpect(jsonPath("$.content[0].subscription.userRef")
                        .value(REGISTERED_CUSTOMER_ID));
    }

    private SubscriptionWithItems getSubscriptionForCustomer(String userRef) {
        SubscriptionWithItems subscriptionWithItems = new SubscriptionWithItems();
        Subscription subscription = new Subscription();
        subscription.setId(ULID.random());
        subscription.setName(ULID.random());
        subscription.setUserRefType(DefaultUserTypes.BLC_CUSTOMER.name());
        subscription.setUserRef(userRef);
        subscriptionWithItems.setSubscription(subscription);
        return subscriptionWithItems;
    }

    protected String getContextRequest(String tenantId) throws JsonProcessingException {
        ContextRequest contextRequest = new ContextRequest();
        contextRequest.setTenantId(tenantId);

        return mapper.writeValueAsString(contextRequest);
    }

    private ContextInfo createContextInfo() {
        return new ContextInfo(OperationType.CREATE,
                new ContextRequest().withTenantId(TENANT));
    }
}
