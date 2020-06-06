package net.plcloud.cat.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * 利用bean前后置处理，动态修改bean
 */
@ConditionalOnClass(name = {"org.apache.rocketmq.client.producer.DefaultMQProducer", "org.apache.rocketmq.spring.core.RocketMQTemplate"})
@Component
public class RocketMQProxyBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof RocketMQTemplate) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(bean.getClass());
            //设置增强
            enhancer.setCallback(new RocketMQTemplateInterceptor());
            RocketMQTemplate rocketMQTemplate = (RocketMQTemplate) enhancer.create();
            rocketMQTemplate.setProducer(((RocketMQTemplate) bean).getProducer());
            rocketMQTemplate.setObjectMapper(((RocketMQTemplate) bean).getObjectMapper());
            return rocketMQTemplate;
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}