/*
 * Copyright (C) 2009 Broadleaf Commerce
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
package com.broadleafcommerce.subscriptionoperation.service.provider.page;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.broadleafcommerce.data.tracking.core.filtering.DefaultNarrowedPageable;
import com.broadleafcommerce.data.tracking.core.filtering.DefaultUnnumberedPageable;
import com.broadleafcommerce.data.tracking.core.filtering.DefaultUntotalledPage;
import com.broadleafcommerce.data.tracking.core.filtering.UnnumberedPageable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates a Page representing the data and page data received as a response from another service.
 * This is necessary since the page and corresponding pageable are interfaces, and their
 * implementations need to be determined based off the data provided in the response.
 *
 * @author Jacob Mitash
 */
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponsePageGenerator<T> {

    @Getter
    private final Page<T> page;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ResponsePageGenerator(
            @JsonProperty("content") List<T> content,
            @JsonProperty("total") Long total,
            @JsonProperty("totalElements") Long totalElements,
            @JsonProperty("pageable") JsonNode rawPageable) {
        Pageable pageable = resolvePageable(rawPageable);
        if (pageable instanceof UnnumberedPageable) {
            page = new DefaultUntotalledPage<>(content, pageable);
        } else {
            total = total == null ? totalElements : total;
            page = new PageImpl<>(content, pageable, total == null ? content.size() : total);
        }
    }

    /**
     * Generates an appropriate pageable based off of the pageable data received.
     *
     * @param rawPageable the raw pageable data to base the pageable implementation off of
     * @return a pageable representing the response data's pageable
     */
    protected Pageable resolvePageable(JsonNode rawPageable) {
        try {
            if (rawPageable.has("underlyingSize")
                    && rawPageable.has("offset")) {
                return DefaultNarrowedPageable.of(
                        rawPageable.get("offset").asLong(),
                        rawPageable.get("pageSize").asInt(),
                        rawPageable.get("forward").asBoolean(true),
                        Sort.unsorted(),
                        rawPageable.get("underlyingSize").asInt());
            } else if (!rawPageable.has("pageNumber")
                    && rawPageable.has("offset")
                    && rawPageable.has("pageSize")) {
                return DefaultUnnumberedPageable.of(
                        rawPageable.get("offset").asLong(),
                        rawPageable.get("pageSize").asInt());
            } else if (rawPageable.has("pageNumber")
                    && rawPageable.has("pageSize")) {
                return PageRequest.of(
                        rawPageable.get("pageNumber").asInt(),
                        rawPageable.get("pageSize").asInt());
            } else {
                return Pageable.unpaged();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve pageable from response: {}", rawPageable, e);
            return Pageable.unpaged();
        }
    }
}
