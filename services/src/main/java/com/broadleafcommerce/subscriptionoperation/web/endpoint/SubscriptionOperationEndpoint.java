package com.broadleafcommerce.subscriptionoperation.web.endpoint;

import com.broadleafcommerce.common.extension.data.DataRouteByKey;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.context.ContextOperation;
import com.broadleafcommerce.data.tracking.core.policy.Policy;
import com.broadleafcommerce.data.tracking.core.type.OperationType;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkPostMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkRestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

import static com.broadleafcommerce.subscriptionoperation.provider.jpa.environment.RouteConstants.Persistence.SUBSCRIPTION_OPS_ROUTE_KEY;

@FrameworkRestController
@FrameworkMapping(SubscriptionOperationEndpoint.BASE_URI)
@DataRouteByKey(SUBSCRIPTION_OPS_ROUTE_KEY)
public class SubscriptionOperationEndpoint {
    public static final String BASE_URI = "/subscription-ops";

    @FrameworkPostMapping
    @Policy(permissionRoots = "SUBSCRIPTION")
    public List<String> createSubscriptions(
            @RequestBody List<String> subscriptionsWithItems,
            @ContextOperation(OperationType.CREATE) final ContextInfo contextInfo
    ) {
        // TODO: Service & provider
        return new ArrayList<>();
    }

    @FrameworkPostMapping
    @Policy(permissionRoots = "SUBSCRIPTION")
    public String createSubscription(
            @RequestBody String subscriptionWithItems,
            @ContextOperation(OperationType.CREATE) final ContextInfo contextInfo
            ) {
        // TODO: Service & provider
        return "";
    }
}
