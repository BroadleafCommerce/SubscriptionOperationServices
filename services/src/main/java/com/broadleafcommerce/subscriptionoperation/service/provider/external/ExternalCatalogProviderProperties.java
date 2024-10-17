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

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("broadleaf.subscriptionoperation.catalogprovider")
public class ExternalCatalogProviderProperties {

    /**
     * The base url for an external service holding products.
     */
    private String url;

    /**
     * The base uri path to the products endpoint
     */
    private String productsUri = "/products";

    /**
     * The base uri path to the products endpoint
     */
    private String productUri = productsUri + "/{productId}";

    /**
     * The service client to use when calling catalog services. Default is "subscriptionopsclient".
     */
    private String serviceClient = "subscriptionopsclient";
}
