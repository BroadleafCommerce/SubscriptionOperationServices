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
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.subscriptionoperation.domain.Product;
import com.broadleafcommerce.subscriptionoperation.domain.SubscriptionWithItems;
import com.broadleafcommerce.subscriptionoperation.service.DefaultSubscriptionOperationService;
import com.broadleafcommerce.subscriptionoperation.service.DefaultSubscriptionValidationService;
import com.broadleafcommerce.subscriptionoperation.service.SubscriptionOperationService;
import com.broadleafcommerce.subscriptionoperation.service.SubscriptionValidationService;
import com.broadleafcommerce.subscriptionoperation.service.modification.CancelSubscriptionHandler;
import com.broadleafcommerce.subscriptionoperation.service.modification.ChangeSubscriptionAutoRenewalHandler;
import com.broadleafcommerce.subscriptionoperation.service.modification.DowngradeSubscriptionHandler;
import com.broadleafcommerce.subscriptionoperation.service.modification.ModifySubscriptionHandler;
import com.broadleafcommerce.subscriptionoperation.service.modification.UpgradeSubscriptionHandler;
import com.broadleafcommerce.subscriptionoperation.service.provider.CatalogProvider;
import com.broadleafcommerce.subscriptionoperation.service.provider.SubscriptionProvider;
import com.broadleafcommerce.subscriptionoperation.service.provider.external.ExternalCartOperationsProvider;
import com.broadleafcommerce.subscriptionoperation.service.provider.external.ExternalCatalogProvider;
import com.broadleafcommerce.subscriptionoperation.service.provider.external.ExternalCatalogProviderProperties;
import com.broadleafcommerce.subscriptionoperation.service.provider.external.ExternalSubscriptionProperties;
import com.broadleafcommerce.subscriptionoperation.service.provider.external.ExternalSubscriptionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
public class SubscriptionOperationServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SubscriptionValidationService subscriptionValidationService(
            CatalogProvider<Product> catalogProvider) {
        return new DefaultSubscriptionValidationService(catalogProvider);
    }

    @Bean
    @ConditionalOnMissingBean(name = "cancelSubscriptionModificationHandler")
    CancelSubscriptionHandler cancelSubscriptionModificationHandler(TypeFactory typeFactory) {
        return new CancelSubscriptionHandler(typeFactory);
    }

    @Bean
    @ConditionalOnMissingBean(name = "upgradeSubscriptionModificationHandler")
    UpgradeSubscriptionHandler upgradeSubscriptionModificationHandler(TypeFactory typeFactory) {
        return new UpgradeSubscriptionHandler(typeFactory);
    }

    @Bean
    @ConditionalOnMissingBean(name = "downgradeSubscriptionModificationHandler")
    DowngradeSubscriptionHandler downgradeSubscriptionModificationHandler(TypeFactory typeFactory) {
        return new DowngradeSubscriptionHandler(typeFactory);
    }

    @Bean
    @ConditionalOnMissingBean(name = "changeSubscriptionAutoRenewalModificationHandler")
    ChangeSubscriptionAutoRenewalHandler changeSubscriptionAutoRenewalModificationHandler(
            TypeFactory typeFactory,
            SubscriptionProvider<SubscriptionWithItems> subscriptionProvider,
            MessageSource messageSource) {
        return new ChangeSubscriptionAutoRenewalHandler(typeFactory,
                subscriptionProvider,
                messageSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public SubscriptionOperationService<SubscriptionWithItems> subscriptionOperationService(
            SubscriptionProvider<SubscriptionWithItems> subscriptionProvider,
            SubscriptionValidationService subscriptionValidationService,
            TypeFactory typeFactory,
            List<ModifySubscriptionHandler> modifySubscriptionHandlers) {
        return new DefaultSubscriptionOperationService<>(subscriptionProvider,
                subscriptionValidationService,
                typeFactory,
                modifySubscriptionHandlers);
    }

    @Configuration
    @EnableConfigurationProperties({ExternalSubscriptionProperties.class,
            ExternalCatalogProviderProperties.class})
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

        @Bean
        @ConditionalOnMissingBean
        public CatalogProvider<Product> subOpsCatalogProvider(
                @Qualifier("subscriptionOperationWebClient") WebClient webClient,
                ObjectMapper objectMapper,
                TypeFactory typeFactory,
                ExternalCatalogProviderProperties properties) {
            return new ExternalCatalogProvider<>(webClient,
                    objectMapper,
                    typeFactory,
                    properties);
        }

        @Bean
        @ConditionalOnMissingBean
        public ExternalCartOperationsProvider subOpsCartOperationsProvider() {
            return new ExternalCartOperationsProvider();
        }
    }
}
