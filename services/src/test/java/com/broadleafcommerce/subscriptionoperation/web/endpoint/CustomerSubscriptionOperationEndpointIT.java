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
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultSubscriptionActionTypes;
import com.broadleafcommerce.subscriptionoperation.domain.enums.DefaultUserRefTypes;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.SubscriptionActionRequest;
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
    protected static final String CUSTOMER_URI = "/customers";
    protected static final String CUSTOMER_READ_URI = "/{customerId}/subscriptions";
    protected static final String CUSTOMER_READ_BY_ID_URI =
            "/{customerId}/subscriptions/{subscriptionId}";

    protected static final String CUSTOMER_SUBSCRIPTION = "CUSTOMER_SUBSCRIPTION";

    protected static final String TENANT = "tenant1";
    protected static final String REGISTERED_CUSTOMER = "registeredCustomer";
    protected static final String REGISTERED_CUSTOMER_ID = "1";

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
                                .value(DefaultUserRefTypes.BLC_CUSTOMER.name()))
                .andExpect(jsonPath("$.content[0].subscription.userRef")
                        .value(REGISTERED_CUSTOMER_ID));
    }

    @Test
    void cannotReadSubscriptionByIdWithoutPermissionAndWrongCustomer() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", "2");

        mockMvc.perform(get(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI, "2", subscriptionId)
                .with(authUtil.withAuthoritiesAndDetails(Sets.newSet("RANDOM_PERMISSION"),
                        authDetails))
                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSubscriptionByIdWithoutPermissionWithCorrectCustomer() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", REGISTERED_CUSTOMER_ID);

        mockMvc.perform(
                get(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI, REGISTERED_CUSTOMER_ID, subscriptionId)
                        .with(authUtil.withAuthoritiesAndDetails(Sets.newSet("RANDOM_PERMISSION"),
                                authDetails))
                        .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSubscriptionByIdWithPermissionWithWrongAuthCustomer() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", "2");

        mockMvc.perform(
                get(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI, REGISTERED_CUSTOMER_ID, subscriptionId)
                        .with(authUtil.withAuthoritiesAndDetails(
                                Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                                authDetails))
                        .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSubscriptionByIdWithPermissionWithWrongCustomer() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", "2");

        mockMvc.perform(get(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI, "2", subscriptionId)
                .with(authUtil.withAuthoritiesAndDetails(Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                        authDetails))
                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotReadSubscriptionByIdWithPermissionWithUnknownSubscriptionId() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", REGISTERED_CUSTOMER_ID);

        mockMvc.perform(get(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI, REGISTERED_CUSTOMER_ID,
                "unknownSubscriptionId")
                        .with(authUtil.withAuthoritiesAndDetails(
                                Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                                authDetails))
                        .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void readsSubscriptionByIdWithPermission() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", REGISTERED_CUSTOMER_ID);

        mockMvc.perform(
                get(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI, REGISTERED_CUSTOMER_ID, subscriptionId)
                        .with(authUtil.withAuthoritiesAndDetails(
                                Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                                authDetails))
                        .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.id").value(subscriptionId))
                .andExpect(jsonPath("$.subscription.userRefType")
                        .value(DefaultUserRefTypes.BLC_CUSTOMER.name()))
                .andExpect(jsonPath("$.subscription.userRef").value(REGISTERED_CUSTOMER_ID));
    }

    @Test
    void cannotReadSubscriptionActionsWithoutPermissionAndWrongCustomer() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        SubscriptionActionRequest request = new SubscriptionActionRequest();
        request.setSubscriptionId(subscriptionId);

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", "2");

        mockMvc.perform(
                post(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI + "/actions", "2", subscriptionId)
                        .with(authUtil.withAuthoritiesAndDetails(Sets.newSet("RANDOM_PERMISSION"),
                                authDetails))
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSubscriptionActionsWithoutPermissionWithCorrectCustomer() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        SubscriptionActionRequest request = new SubscriptionActionRequest();
        request.setSubscriptionId(subscriptionId);


        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", REGISTERED_CUSTOMER_ID);

        mockMvc.perform(
                post(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI + "/actions", REGISTERED_CUSTOMER_ID,
                        subscriptionId)
                                .with(authUtil.withAuthoritiesAndDetails(
                                        Sets.newSet("RANDOM_PERMISSION"),
                                        authDetails))
                                .contentType(APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSubscriptionActionsWithPermissionWithWrongAuthCustomer() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        SubscriptionActionRequest request = new SubscriptionActionRequest();
        request.setSubscriptionId(subscriptionId);

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", "2");

        mockMvc.perform(
                post(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI + "/actions", REGISTERED_CUSTOMER_ID,
                        subscriptionId)
                                .with(authUtil.withAuthoritiesAndDetails(
                                        Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                                        authDetails))
                                .contentType(APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSubscriptionActionsWithPermissionWithWrongCustomer() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        SubscriptionActionRequest request = new SubscriptionActionRequest();
        request.setSubscriptionId(subscriptionId);

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", "2");

        mockMvc.perform(
                post(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI + "/actions", "2", subscriptionId)
                        .with(authUtil.withAuthoritiesAndDetails(
                                Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                                authDetails))
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotReadSubscriptionActionsWithPermissionWithUnknownSubscriptionId() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        SubscriptionActionRequest request = new SubscriptionActionRequest();

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", REGISTERED_CUSTOMER_ID);

        mockMvc.perform(
                post(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI + "/actions", REGISTERED_CUSTOMER_ID,
                        "unknownSubscriptionId")
                                .with(authUtil.withAuthoritiesAndDetails(
                                        Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                                        authDetails))
                                .contentType(APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void readsSubscriptionActionsWithPermission() throws Exception {
        SubscriptionWithItems subscriptionWithItems =
                getSubscriptionForCustomer(REGISTERED_CUSTOMER_ID);
        String subscriptionId = subscriptionWithItems.getSubscription().getId();
        inMemorySubscriptionProvider.create(subscriptionWithItems,
                createContextInfo());

        SubscriptionActionRequest request = new SubscriptionActionRequest();
        request.setSubscriptionId(subscriptionId);

        Map<String, Object> authDetails = new HashMap<>();
        authDetails.put("customer_id", REGISTERED_CUSTOMER_ID);

        mockMvc.perform(
                post(CUSTOMER_URI + CUSTOMER_READ_BY_ID_URI + "/actions", REGISTERED_CUSTOMER_ID,
                        subscriptionId)
                                .with(authUtil.withAuthoritiesAndDetails(
                                        Sets.newSet("READ_CUSTOMER_SUBSCRIPTION"),
                                        authDetails))
                                .contentType(APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                                .header(CONTEXT_REQUEST_HEADER, getContextRequest(TENANT))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // TODO: Update tests once action logic is implemented
                .andExpect(jsonPath("$.availableActions[*].actionType",
                        contains(DefaultSubscriptionActionTypes.EDIT.name(),
                                DefaultSubscriptionActionTypes.CHANGE_AUTO_RENEWAL.name(),
                                DefaultSubscriptionActionTypes.UPGRADE.name(),
                                DefaultSubscriptionActionTypes.PAUSE.name(),
                                DefaultSubscriptionActionTypes.RESUME.name(),
                                DefaultSubscriptionActionTypes.SUSPEND.name(),
                                DefaultSubscriptionActionTypes.TERMINATE.name(),
                                DefaultSubscriptionActionTypes.REACTIVATE.name(),
                                DefaultSubscriptionActionTypes.CANCEL.name())))
                .andExpect(jsonPath("$.unavailableReasonsByActionType.DOWNGRADE[0]")
                        .value("Downgrade is not supported"));
    }

    private SubscriptionWithItems getSubscriptionForCustomer(String userRef) {
        SubscriptionWithItems subscriptionWithItems = new SubscriptionWithItems();
        Subscription subscription = new Subscription();
        subscription.setId(ULID.random());
        subscription.setName(ULID.random());
        subscription.setUserRefType(DefaultUserRefTypes.BLC_CUSTOMER.name());
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
