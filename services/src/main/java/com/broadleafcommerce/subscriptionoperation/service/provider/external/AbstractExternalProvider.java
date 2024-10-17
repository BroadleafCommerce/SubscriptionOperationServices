/*
 * Copyright (C) 2020 Broadleaf Commerce
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

import static com.broadleafcommerce.data.tracking.core.context.ContextInfoHandlerMethodArgumentResolver.CONTEXT_REQUEST_HEADER;
import static com.broadleafcommerce.data.tracking.core.context.ContextInfoHandlerMethodArgumentResolver.IGNORE_TRANSLATION_HEADER;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.broadleafcommerce.common.error.ApiError;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.exception.EntityMissingException;
import com.broadleafcommerce.data.tracking.core.filtering.NarrowedPageable;
import com.broadleafcommerce.data.tracking.core.filtering.UnnumberedPageable;
import com.broadleafcommerce.subscriptionoperation.exception.ProviderApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractExternalProvider {

    /**
     * The {@link ApiError#getType()} that indicates entity is not found.
     */
    public final static String ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";

    protected static final String RSQL_FILTER_PARAM = "cq";

    @Getter(AccessLevel.PROTECTED)
    private final WebClient webClient;

    @Getter(AccessLevel.PROTECTED)
    private final ObjectMapper objectMapper;

    @Getter(AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    /**
     * Builds the headers to be passed along with the request to the provider.
     *
     * @param contextInfo {@link ContextInfo} from the original request containing tenant and
     *        sandbox info
     *
     * @return The headers to be passed along with the request to the provider.
     */
    protected HttpHeaders getHeaders(@Nullable final ContextInfo contextInfo) {
        final HttpHeaders headers = new HttpHeaders();

        if (contextInfo == null) {
            return headers;
        }

        if (contextInfo.getLocale() != null) {
            headers.setAcceptLanguageAsLocales(Collections.singletonList(contextInfo.getLocale()));
        }
        headers.add(IGNORE_TRANSLATION_HEADER, String.valueOf(contextInfo.isIgnoreTranslation()));

        try {
            headers.add(CONTEXT_REQUEST_HEADER,
                    objectMapper.writeValueAsString(contextInfo.getContextRequest()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to convert to JSON", e);
        }

        return headers;
    }

    /**
     * Converts a pageable into its corresponding query parameters.
     *
     * @param pageable the pageable to convert
     * @return a map of query parameters
     */
    protected MultiValueMap<String, String> pageableToParams(@Nullable Pageable pageable) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        if (pageable == null) {
            return params;
        }

        pageable.getSort()
                .forEach(sortOrder -> params.add("sort", sortOrderToParamValue(sortOrder)));

        if (pageable instanceof NarrowedPageable) {
            params.add("forward", String.valueOf(((NarrowedPageable) pageable).isForward()));
        }

        if (pageable.isUnpaged()) {
            // It's unpaged so get all results.
            // Unpaged is not the same as null: Null would indicate to use the defaults configured
            // at the endpoint. Whereas unpaged means all the results should be returned.
            if (pageable instanceof UnnumberedPageable) {
                params.add("offset", "0");
            } else {
                params.add("page", "0");
            }
            params.add("size", String.valueOf(Integer.MAX_VALUE));

            return params;
        }

        addOffsetOrPageNumber(params, pageable);

        params.add("size", String.valueOf(pageable.getPageSize()));

        return params;
    }

    protected void addOffsetOrPageNumber(@lombok.NonNull MultiValueMap<String, String> params,
            @lombok.NonNull Pageable pageable) {
        if (pageable instanceof UnnumberedPageable) {
            params.add("offset", String.valueOf(pageable.getOffset()));
        } else {
            try {
                params.add("page", String.valueOf(pageable.getPageNumber()));
            } catch (UnsupportedOperationException ignored) {
                // just means this particular implementation of Pageable doesn't support page number
                // example is UnnumberedPageable
            }
        }
    }

    /**
     * Converts a sort order into a query parameter value (e.g. name,asc).
     *
     * @param order the sort order to convert
     * @return the query parameter value for the sort order
     */
    private String sortOrderToParamValue(Sort.Order order) {
        return order.getProperty() + "," + order.getDirection().toString().toLowerCase();
    }

    /**
     * Executes a request with default Web Client error handling.
     *
     * @param request the request to execute
     * @param <T> the return type of the request operation
     * @return the value generated by the supplier
     */
    protected <T> T executeRequest(Supplier<T> request) {
        try {
            return request.get();
        } catch (WebClientResponseException.NotFound nfe) {
            throw buildNotFoundException(nfe);
        } catch (WebClientResponseException e) {
            throw new ProviderApiException(e);
        }
    }

    /**
     * Builds a not found exception that correlates to the given
     * {@link WebClientResponseException.NotFound} exception.
     * <p>
     * If the exception is of type {@link #ENTITY_NOT_FOUND}, an {@link EntityMissingException} is
     * thrown. Otherwise, it's wrapped in {@link ProviderApiException}.
     *
     * @param nfe the {@link WebClientResponseException.NotFound} to build the not found exception
     *        from
     * @return a not found exception that correlates to the given
     *         {@link WebClientResponseException.NotFound} exception
     */
    protected RuntimeException buildNotFoundException(WebClientResponseException.NotFound nfe) {
        try {
            ResponseEntity<ApiError> response = objectMapper
                    .readValue(nfe.getResponseBodyAsString(), ApiError.class)
                    .toResponseEntity();

            if (isEntityNotFound(response)) {
                return new EntityMissingException();
            } else {
                return new ProviderApiException(nfe);
            }
        } catch (JsonProcessingException ignored) {
            return new ProviderApiException(nfe);
        }
    }

    /**
     * Determines if the given {@link ResponseEntity} indicates entity not found.
     * <p>
     * This is useful to distinguish a {@link HttpStatus#NOT_FOUND} response indicating entity
     * cannot be found from a response indicating the endpoint/url cannot be found.
     *
     * @param apiError the {@link ResponseEntity} to check against
     * @return true if the given {@link ResponseEntity} indicates entity not found
     */
    protected boolean isEntityNotFound(ResponseEntity<ApiError> apiError) {
        return Optional.ofNullable(apiError.getBody())
                .map(errorBody -> StringUtils.equals(ENTITY_NOT_FOUND, errorBody.getType()))
                .orElse(false);
    }
}
