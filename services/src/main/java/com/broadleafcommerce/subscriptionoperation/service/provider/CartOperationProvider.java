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
package com.broadleafcommerce.subscriptionoperation.service.provider;

import org.springframework.lang.Nullable;

import com.broadleafcommerce.cart.client.domain.Cart;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.subscriptionoperation.web.domain.CreateCartRequest;

/**
 * Component responsible for interacting with a Cart Operations provider to act against user carts.
 *
 * @author Nathan Moore (nathandmoore)
 */
public interface CartOperationProvider<C extends Cart> {

    /**
     * Method used to create a new {@link Cart}.
     *
     * @param request The information needed to create a new cart.
     * @param contextInfo The sandbox and multitenant context
     * @return The created cart.
     */
    C createCart(CreateCartRequest request, @Nullable ContextInfo contextInfo);

}
