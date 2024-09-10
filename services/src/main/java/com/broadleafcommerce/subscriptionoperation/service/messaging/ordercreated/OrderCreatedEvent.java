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
package com.broadleafcommerce.subscriptionoperation.service.messaging.ordercreated;

import org.springframework.lang.Nullable;

import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.order.client.domain.Order;
import com.broadleafcommerce.order.client.domain.OrderFulfillment;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A message event dispatched after an order has been created successfully.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The order object that was created
     */
    private Order order;

    /**
     * The fulfillments created for the order
     */
    private List<OrderFulfillment> fulfillments;

    /**
     * The {@link ContextInfo} derived from the original request containing tenant and sandbox info.
     *
     * @return The {@link ContextInfo} derived from the original request
     */
    @Nullable
    private ContextInfo contextInfo;

}
