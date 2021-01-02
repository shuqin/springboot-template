package cc.lovesq.strategy;

import cc.lovesq.constants.DeliveryType;
import cc.lovesq.goodssnapshot.ServiceTpl;
import cc.lovesq.model.Order;
import org.springframework.stereotype.Component;

/**
 * @Description TODO
 * @Date 2021/1/2 3:14 下午
 * @Created by qinshu
 */
@Component
public class SelfetchServiceDescStrategy implements ServiceDescStrategy {
    @Override
    public boolean isSatisfied(Order order, ServiceTpl config) {
        return config.getKey().equals("selfetch") && order.getDeliveryType() == DeliveryType.selfetch;
    }
}
