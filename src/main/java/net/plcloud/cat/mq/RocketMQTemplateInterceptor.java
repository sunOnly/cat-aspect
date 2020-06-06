package net.plcloud.cat.mq;

import com.alibaba.fastjson.JSON;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import net.plcloud.cat.util.CatAopUtils;
import net.plcloud.cat.util.CatUtil;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class RocketMQTemplateInterceptor implements MethodInterceptor {

    @Override
    public Object intercept(Object sub, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        Transaction transaction = null;
        String name = "";
        //1、处理cat异常的情况
        try {
            name = methodProxy.getSignature().getName();
            transaction = CatUtil.initTransaction("RocketMqProducer", name);
            if (CatAopUtils.isPrintParams) {
                CatUtil.logEvent("Args", name, "0", getParams(method, objects));
            }
        } catch (Exception e) {
            Cat.logEvent(this.getClass().getName(), e.getMessage());
        }

        //2、调用核心逻辑
        Object retVal = null;
        Throwable ex = null;
        try {
            retVal = methodProxy.invokeSuper(sub, objects);
        } catch (Throwable e) {
            ex = e;
            throw e;
        } finally {
            try {
                if (ex != null) {
                    if (!CatAopUtils.isPrintParams) {
                        CatUtil.logEvent("Args", name, "0", getParams(method, objects));
                    }
                    CatUtil.errorCat(transaction, ex);
                } else {
                    if (CatAopUtils.isPrintResult) {
                        Cat.logEvent("Result", name, "0", JSON.toJSONString(retVal));
                    }
                    CatUtil.successCat(transaction);
                }
            } catch (Throwable e) {
                CatUtil.successCat(transaction, name, e);
            }
        }
        return retVal;
    }

    public static String getParams(Method method, Object[] objects) {
        String message = "";

        try {
            StringBuilder sbf = new StringBuilder();
            int var4 = objects.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                Object o = objects[var5];
                if (o != null) {
                    sbf.append(",").append(JSON.toJSONString(o));
                }
            }

            if (sbf.length() > 1) {
                message = message + "(" + sbf.toString().substring(1) + ")";
            }
        } catch (Exception var7) {
            Cat.getProducer().logEvent("CatAopUtils", var7.getMessage());
        }

        return message;
    }


}
