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
package com.broadleafcommerce.subscriptionoperation.service.autoconfigure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.common.extension.data.DataRouteSupporting;
import com.broadleafcommerce.common.extension.data.PackageDataRouteSupplier;
import com.broadleafcommerce.data.tracking.core.context.ContextInfoCustomizer;
import com.broadleafcommerce.subscriptionoperation.domain.Subscription;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionItem;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.provider.jpa.environment.RouteConstants;
import com.broadleafcommerce.subscriptionoperation.service.DefaultSubscriptionOperationService;
import com.broadleafcommerce.subscriptionoperation.service.SubscriptionOperationService;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.service.provider.external.ExternalSubscriptionProperties;
import com.broadleafcommerce.subscriptionoperation.service.provider.external.ExternalSubscriptionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
public class SubscriptionOperationServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SubscriptionOperationService<Subscription, SubscriptionItem, SubscriptionWithItems> subscriptionOperationService(
            SubscriptionProvider<SubscriptionWithItems> subscriptionProvider,
            TypeFactory typeFactory) {
        return new DefaultSubscriptionOperationService<>(subscriptionProvider, typeFactory);
    }

    @Configuration
    @EnableConfigurationProperties({ExternalSubscriptionProperties.class})
    public static class SubscriptionProviderConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public SubscriptionProvider<SubscriptionWithItems> subscriptionProvider(
                @Qualifier("subscriptionOperationWebClient") WebClient webClient,
                ObjectMapper objectMapper,
                TypeFactory typeFactory,
                ExternalSubscriptionProperties properties) {
            return new ExternalSubscriptionProvider<>(webClient,
                    objectMapper,
                    typeFactory,
                    properties);
        }
    }

    /**
     * Defines a {@link DataRouteSupporting} for Subscription Ops. By default, this is detached from
     * any persistence and is used for supporting {@link ContextInfoCustomizer} in a flexpackage
     * configuration to ensure it is only invoked for this specific service.
     */
    @Bean(name = "subscriptionOperationSource")
    @ConditionalOnMissingBean(name = "subscriptionOperationSource")
    public DataRouteSupporting subscriptionOperationSource() {
        return new OrchestrationDataRouteSupporting(
                RouteConstants.Persistence.SUBSCRIPTION_OPS_ROUTE_KEY);
    }

    @Bean(name = "subscriptionOperationRouteSupplier")
    @ConditionalOnMissingBean(name = "subscriptionOperationRouteSupplier")
    public PackageDataRouteSupplier<DataRouteSupporting> subscriptionOperationRouteSupplier(
            @Nullable @Qualifier("subscriptionOperationSource") DataRouteSupporting route) {
        return () -> new PackageDataRouteSupplier.PackageMapping<>(
                RouteConstants.Persistence.SUBSCRIPTION_OPS_ROUTE_PACKAGE,
                route);
    }

    @RequiredArgsConstructor
    private static class OrchestrationDataRouteSupporting implements DataRouteSupporting {

        private final String routeKey;

        @Override
        public String getLookupKey() {
            return routeKey;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

}
