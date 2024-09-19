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
package com.broadleafcommerce.subscriptionoperation.service.provider.external;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.exception.EntityMissingException;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.Getter;

public class ExternalSubscriptionProvider<S extends SubscriptionWithItems>
        extends AbstractExternalProvider
        implements SubscriptionProvider<S> {

    protected static final String SUBSCRIPTION_SOURCE_PARAM = "subscriptionSource";
    protected static final String SUBSCRIPTION_SOURCE_REF_PARAM = "subscriptionSourceRef";
    protected static final String INCLUDE_ITEMS_PARAM = "includeItems";

    @Getter(AccessLevel.PROTECTED)
    private final ExternalSubscriptionProperties properties;


    public ExternalSubscriptionProvider(WebClient webClient, ObjectMapper objectMapper,
            TypeFactory typeFactory, ExternalSubscriptionProperties properties) {
        super(webClient, objectMapper, typeFactory);
        this.properties = properties;
    }

    @Override
    public S create(S subscriptionWithItems,
            @Nullable ContextInfo contextInfo) {
        return executeRequest(() -> getWebClient()
                .post()
                .uri(getBaseUri().toUriString())
                .headers(headers -> headers.putAll(getHeaders(contextInfo)))
                .attributes(clientRegistrationId(getServiceClient()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subscriptionWithItems)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<S>() {})
                .blockOptional()
                .orElseThrow(EntityMissingException::new));
    }

    /**
     * Gets the base URI common to all requests this provider will make.
     *
     * @return a URI components builder with the base URI set up
     */
    protected UriComponentsBuilder getBaseUri() {
        return UriComponentsBuilder.fromHttpUrl(properties.getUrl())
                .path(properties.getSubscriptionUri());
    }


    protected String getServiceClient() {
        return properties.getServiceClient();
    }
}
