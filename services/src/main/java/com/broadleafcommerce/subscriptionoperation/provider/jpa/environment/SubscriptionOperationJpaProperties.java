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
package com.broadleafcommerce.subscriptionoperation.provider.jpa.environment;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.broadleafcommerce.common.jpa.data.JpaProperties;
import com.broadleafcommerce.common.jpa.data.JpaPropertyRelated;
import com.broadleafcommerce.common.jpa.data.SchemaDelegatingProperties;

import lombok.Data;

@Data
@ConfigurationProperties("broadleaf.subscriptionoperation")
public class SubscriptionOperationJpaProperties implements JpaPropertyRelated {

    private JpaProperties jpa = new JpaProperties();

    private DataSourceProperties datasource = new DataSourceProperties();

    private LiquibaseProperties liquibase = new LiquibaseProperties();

    private SchemaDelegatingProperties delegating = new SchemaDelegatingProperties();

}
