/*
 * Copyright (C) 2021 Broadleaf Commerce
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
package com.broadleafcommerce.subscriptionoperation.web.endpoint.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.broadleafcommerce.common.error.ApiError;
import com.broadleafcommerce.common.error.validation.web.FrameworkExceptionAdvisor;
import com.broadleafcommerce.subscriptionoperation.exception.ProviderApiException;
import com.broadleafcommerce.subscriptionoperation.service.exception.IllegalResponseException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InsufficientSubscriptionAccessException;
import com.broadleafcommerce.subscriptionoperation.service.exception.InvalidSubscriptionCreationRequestException;
import com.broadleafcommerce.subscriptionoperation.service.exception.UnsupportedSubscriptionModificationRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * General exception handler registry for handlers not exclusive to a single controller.
 */
@Slf4j
@FrameworkExceptionAdvisor
@RestControllerAdvice(annotations = ResponseBody.class)
@RequiredArgsConstructor
public class SubscriptionOperationExceptionAdvisor {

    @Getter(AccessLevel.PROTECTED)
    @Setter(onMethod_ = {@Autowired})
    private ObjectMapper mapper;

    @ExceptionHandler
    public ResponseEntity<ApiError> handleInvalidSubscriptionCreationRequestException(
            InvalidSubscriptionCreationRequestException ex,
            WebRequest request) {
        logDebug(ex, request);
        return new ApiError("INVALID_SUBSCRIPTION_CREATION_REQUEST",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST)
                        .toResponseEntity();
    }

    @ExceptionHandler
    public ResponseEntity<ApiError> handleInvalidSubscriptionAccessException(
            InsufficientSubscriptionAccessException ex,
            WebRequest request) {
        logDebug(ex, request);
        return new ApiError("INSUFFICIENT_SUBSCRIPTION_ACCESS",
                ex.getMessage(),
                HttpStatus.FORBIDDEN)
                        .toResponseEntity();
    }

    @ExceptionHandler({ProviderApiException.class})
    public ResponseEntity<ApiError> handleProviderApiError(ProviderApiException ex,
            WebRequest request) {
        final WebClientResponseException receivedException = ex.getReceivedException();

        if (receivedException != null) {
            try {
                String rawResponse = receivedException.getResponseBodyAsString();
                ResponseEntity<ApiError> apiError = mapper.readValue(rawResponse,
                        ApiError.class)
                        .toResponseEntity();
                String requestURL =
                        ((ServletWebRequest) request).getRequest().getRequestURL().toString();
                log.error("Request to {} raised an exception: {}", requestURL, rawResponse);
                return apiError;
            } catch (JsonProcessingException ignored) {
                logError(ex, request);
                return new ApiError("PROVIDER_API_ERROR",
                        receivedException.getMessage(),
                        receivedException.getStatusCode())
                                .toResponseEntity();
            }
        }

        logError(ex, request);
        return new ApiError("PROVIDER_API_ERROR", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR)
                .toResponseEntity();
    }

    @ExceptionHandler(UnsupportedSubscriptionModificationRequestException.class)
    public ResponseEntity<ApiError> handleUnsupportedSubscriptionModificationRequestException(
            UnsupportedSubscriptionModificationRequestException ex,
            WebRequest request) {
        logError(ex, request);
        return new ApiError("UNSUPPORTED_MODIFICATION_REQUEST",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST)
                        .toResponseEntity();
    }

    @ExceptionHandler(IllegalResponseException.class)
    public ResponseEntity<ApiError> handleIllegalResponseException(IllegalResponseException ex,
            WebRequest request) {
        logError(ex, request);
        return new ApiError("ILLEGAL_API_RESPONSE",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR)
                        .toResponseEntity();
    }

    protected void logDebug(Exception ex, WebRequest request) {
        String requestURL =
                ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        log.debug("Request to %s raised an exception".formatted(requestURL), ex);
    }

    protected void logError(Exception ex, WebRequest request) {
        String requestURL =
                ((ServletWebRequest) request).getRequest().getRequestURL().toString();
        log.error("Request to %s raised an exception".formatted(requestURL), ex);
    }
}
