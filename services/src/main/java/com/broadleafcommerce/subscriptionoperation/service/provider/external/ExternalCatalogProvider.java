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

import static com.broadleafcommerce.data.tracking.core.filtering.fetch.rsql.RsqlSearchOperation.IN;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.exception.EntityMissingException;
import com.broadleafcommerce.subscriptionoperation.domain.Product;
import com.broadleafcommerce.subscriptionoperation.service.provider.CatalogProvider;
import com.broadleafcommerce.subscriptionoperation.service.provider.page.ResponsePageGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.Node;
import lombok.AccessLevel;
import lombok.Getter;

public class ExternalCatalogProvider<P extends Product> extends AbstractExternalProvider
        implements CatalogProvider<P> {

    @Getter(AccessLevel.PROTECTED)
    private final ExternalCatalogProviderProperties properties;

    public ExternalCatalogProvider(WebClient webClient, ObjectMapper objectMapper,
            TypeFactory typeFactory, ExternalCatalogProviderProperties properties) {
        super(webClient, objectMapper, typeFactory);
        this.properties = properties;
    }

    @Override
    public P readProductById(String productId,
            @Nullable ContextInfo contextInfo) {
        String uri = getBaseUri()
                .path(properties.getProductUri())
                .uriVariables(uriVars("productId", productId))
                .toUriString();

        return executeRequest(() -> getWebClient()
                .get()
                .uri(uri)
                .headers(headers -> headers.putAll(getHeaders(contextInfo)))
                .attributes(clientRegistrationId(getServiceClient()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<P>() {})
                .blockOptional()
                .orElseThrow(EntityMissingException::new));
    }

    @Override
    public Page<P> readProductsByIds(@lombok.NonNull List<String> productIds,
            @Nullable Pageable pageable,
            @Nullable ContextInfo contextInfo) {
        Node filters = buildReadProductsByIdsFilters(productIds);

        String uri = getBaseUri()
                .queryParam(RSQL_FILTER_PARAM, filters)
                .queryParams(pageableToParams(pageable))
                .toUriString();

        return executeRequest(() -> getWebClient()
                .get()
                .uri(uri)
                .headers(headers -> headers.putAll(getHeaders(contextInfo)))
                .attributes(clientRegistrationId(getServiceClient()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(getPageType())
                .blockOptional()
                .map(ResponsePageGenerator::getPage)
                .orElseThrow(EntityMissingException::new));
    }

    /**
     * Build the {@link Node RSQL filters} used for reading the products by IDs.
     *
     * @param productIds the list of product ids.
     * @return the {@link Node RSQL filters} used for reading the products by IDs
     */
    protected Node buildReadProductsByIdsFilters(@lombok.NonNull List<String> productIds) {
        List<Node> allFilters = new ArrayList<>();
        ComparisonNode idsIn =
                new ComparisonNode(IN.getOperator(), "contextId", productIds);
        allFilters.add(idsIn);
        return new AndNode(allFilters);
    }

    /**
     * Gets the type reference for a page generator of item list items.
     *
     * @return type reference for a page generator of item list items
     */
    protected ParameterizedTypeReference<ResponsePageGenerator<P>> getPageType() {
        return new ParameterizedTypeReference<>() {};
    }

    /**
     * Gets the base URI common to all requests this provider will make.
     *
     * @return a URI components builder with the base URI set up
     */
    protected UriComponentsBuilder getBaseUri() {
        return UriComponentsBuilder.fromHttpUrl(properties.getUrl())
                .path(properties.getProductsUri());
    }

    protected String getServiceClient() {
        return properties.getServiceClient();
    }
}
