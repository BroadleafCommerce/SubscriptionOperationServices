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
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.broadleafcommerce.subscriptionoperation.oauth2.client.endpoint.OAuth2ClientCredentialsAccessTokenResponseClient;
import com.broadleafcommerce.subscriptionoperation.oauth2.client.web.SynchronizedDelegatingOAuth2AuthorizedClientManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.function.Supplier;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties({SSLVerificationProperties.class})
public class SubscriptionOperationWebClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "subscriptionOperationWebClient")
    public WebClient subscriptionOperationWebClient(
            @Qualifier("oAuth2FilterFunctionSupplier") Supplier<ServletOAuth2AuthorizedClientExchangeFilterFunction> oauth2FilterSupplier,
            ObjectMapper objectMapper,
            @Qualifier("subscriptionOperationClientHttpConnector") Optional<ClientHttpConnector> clientHttpConnector)
            throws SSLException {
        // Add our own object mapper
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                }).build();

        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        WebClient.Builder webClientBuilder = WebClient.builder();
        clientHttpConnector.ifPresent(webClientBuilder::clientConnector);

        return webClientBuilder
                .uriBuilderFactory(uriBuilderFactory)
                .exchangeStrategies(strategies)
                .apply(oauth2FilterSupplier.get().oauth2Configuration())
                .build();
    }

    /**
     * We'll leave this with a general name as it can (and should) be reused in a flex package.
     *
     * @param clientRegistrations
     * @param clientHttpConnector
     * @return
     */
    @Bean(name = "oAuth2FilterFunctionSupplier")
    @ConditionalOnMissingBean(name = "oAuth2FilterFunctionSupplier")
    public Supplier<ServletOAuth2AuthorizedClientExchangeFilterFunction> subscriptionOperationOauth2FilterFunctionSupplier(
            ClientRegistrationRepository clientRegistrations,
            @Qualifier("subscriptionOperationClientHttpConnector") Optional<ClientHttpConnector> clientHttpConnector) {
        final SynchronizedDelegatingOAuth2AuthorizedClientManager manager =
                new SynchronizedDelegatingOAuth2AuthorizedClientManager(clientRegistrations);
        manager.setAuthorizedClientProvider(
                getClientCredentialsAuthorizedClientProvider(clientHttpConnector));
        return () -> new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
    }

    @Bean
    @ConditionalOnMissingBean(name = "subscriptionOperationClientHttpConnector")
    public ClientHttpConnector subscriptionOperationClientHttpConnector(
            SSLVerificationProperties sslVerificationProperties) throws SSLException {
        if (sslVerificationProperties.isDisabled()) {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            HttpClient httpClient = HttpClient
                    .create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
            return new ReactorClientHttpConnector(httpClient);
        }

        return null; // WebClient builder will initialize the default ClientHttpConnector
    }

    protected static OAuth2AuthorizedClientProvider getClientCredentialsAuthorizedClientProvider(
            Optional<ClientHttpConnector> clientHttpConnector) {
        return OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials(builder -> {
            WebClient.Builder webClientBuilder = WebClient.builder();
            clientHttpConnector.ifPresent(webClientBuilder::clientConnector);

            builder.accessTokenResponseClient(
                    new OAuth2ClientCredentialsAccessTokenResponseClient(
                            webClientBuilder.build()));
        }).build();
    }
}
