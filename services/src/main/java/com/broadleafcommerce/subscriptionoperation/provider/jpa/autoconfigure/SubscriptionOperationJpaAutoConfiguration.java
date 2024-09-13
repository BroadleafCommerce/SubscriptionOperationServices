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
package com.broadleafcommerce.subscriptionoperation.provider.jpa.autoconfigure;

import static com.broadleafcommerce.subscriptionoperation.provider.jpa.environment.RouteConstants.Persistence.SUBSCRIPTION_OP_ROUTE_KEY;
import static com.broadleafcommerce.subscriptionoperation.provider.jpa.environment.RouteConstants.Persistence.SUBSCRIPTION_OP_ROUTE_PACKAGE;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.broadleafcommerce.common.jpa.data.JpaDataRoute;
import com.broadleafcommerce.common.messaging.data.MessagingDataRouteSupporting;
import com.broadleafcommerce.data.tracking.core.data.TrackingDataRouteSupporting;
import com.broadleafcommerce.subscriptionoperation.provider.jpa.environment.SubscriptionOperationJpaProperties;

@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class SubscriptionOperationJpaAutoConfiguration {

    @ConditionalOnProperty(
            name = {"broadleaf.database.provider",
                    "broadleaf.subscriptionoperation.database.provider"},
            havingValue = "jpa")
    @EnableConfigurationProperties(SubscriptionOperationJpaProperties.class)
    @JpaDataRoute(boundPropertiesType = SubscriptionOperationJpaProperties.class,
            routePackage = SUBSCRIPTION_OP_ROUTE_PACKAGE, routeKey = SUBSCRIPTION_OP_ROUTE_KEY,
            supportingRouteTypes = {TrackingDataRouteSupporting.class,
                    MessagingDataRouteSupporting.class})
    public static class EnabledGranularOrFlex {}

}
