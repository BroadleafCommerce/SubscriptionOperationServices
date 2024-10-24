/*
 * Copyright (C) 2009 - 2020 Broadleaf Commerce
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.resource.security.utils.service.AuthenticationUtils;
import com.broadleafcommerce.subscriptionoperation.exception.ProviderApiException;
import com.broadleafcommerce.subscriptionoperation.service.exception.IllegalResponseException;
import com.broadleafcommerce.subscriptionoperation.service.provider.CartOperationProvider;
import com.broadleafcommerce.subscriptionoperation.web.domain.CreateCartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link CartOperationProvider} for interacting with an external service.
 *
 * @author Nathan Moore (nathandmoore)
 */
public class ExternalCartOperationProvider<C extends Cart> extends AbstractExternalProvider
        implements CartOperationProvider<C> {

    @Getter(AccessLevel.PROTECTED)
    private final ExternalCartOperationProviderProperties properties;

    @Getter(AccessLevel.PROTECTED)
    private final AuthenticationUtils authenticationUtils;

    @Getter(AccessLevel.PROTECTED)
    @Setter(onMethod_ = {@Autowired})
    private HttpServletRequest httpServletRequest;

    public ExternalCartOperationProvider(WebClient webClient,
            ObjectMapper objectMapper,
            TypeFactory typeFactory,
            ExternalCartOperationProviderProperties properties,
            AuthenticationUtils authenticationUtils) {
        super(webClient, objectMapper, typeFactory);
        this.properties = properties;
        this.authenticationUtils = authenticationUtils;
    }

    @Override
    public C createCart(@lombok.NonNull CreateCartRequest request,
            @Nullable ContextInfo contextInfo) {
        String uri = getBaseUri()
                .toUriString();
        boolean isAuthenticated = !authenticationUtils.userIsAnonymous();

        return executeRequest(() -> getWebClient()
                .post()
                .uri(uri)
                .headers(headers -> {
                    if (isAuthenticated) {
                        // pass this user's bearer token instead of the service's
                        headers.put(HttpHeaders.AUTHORIZATION, getAuthorizationHeader());
                    }
                    headers.putAll(getHeaders(contextInfo));
                })
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .attributes(isAuthenticated ? attributes -> {
                    // noop
                } : clientRegistrationId(getServiceClient()))
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.createException().flatMap(
                                exception -> Mono.just(new ProviderApiException(exception)))))
                .bodyToMono(getCartType())
                .blockOptional()
                .orElseThrow(() -> new IllegalResponseException(
                        "Response to create cart request did not provide the created cart."));
    }

    protected List<String> getAuthorizationHeader() {
        return List.of(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION));
    }

    /**
     * Gets the base URI common to all requests this provider will make.
     *
     * @return a URI components builder with the base URI set up
     */
    protected UriComponentsBuilder getBaseUri() {
        return UriComponentsBuilder.fromHttpUrl(properties.getUrl())
                .path(properties.getManageCartsUri());
    }

    /**
     * Gets the name of the authorized client for this service making the call.
     *
     * @return The name of the authorized client for this service making the call.
     */
    protected String getServiceClient() {
        return properties.getServiceClient();
    }

    /**
     * Gets the type reference for a page generator of item list items.
     *
     * @return type reference for a page generator of item list items
     */
    protected ParameterizedTypeReference<C> getCartType() {
        return new ParameterizedTypeReference<>() {};
    }
}
