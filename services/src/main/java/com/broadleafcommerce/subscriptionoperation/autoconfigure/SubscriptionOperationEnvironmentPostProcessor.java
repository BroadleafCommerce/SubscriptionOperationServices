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
package com.broadleafcommerce.subscriptionoperation.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * Spring Boot default property overrides for this service
 */
public class SubscriptionOperationEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String YAML_CLASSPATH_LOCATION = "subscriptionoperation-defaults.yml";

    private YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
            SpringApplication application) {
        try {
            loader.load("Broadleaf Subscription Operation Services -- Defaults",
                    new ClassPathResource(YAML_CLASSPATH_LOCATION))
                    .forEach(ps -> environment.getPropertySources().addLast(ps));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not load the Subscription Operation services property defaults",
                    e);
        }
    }

}
