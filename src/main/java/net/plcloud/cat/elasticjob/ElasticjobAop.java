package net.plcloud.cat.elasticjob;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import net.plcloud.cat.TraceContext;
import net.plcloud.cat.util.CatAopUtils;
import net.plcloud.cat.util.CatUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author : wangbiao
 * @version V1.0
 * @Project: xyy-common-filter
 * @Package com.xyy.cat.elasticjob
 * @Description: 对分布式任务调度elasticjob的执行次数和耗时做切面监控
 * @date Date : 2020年04月08日 18:24
 */
@Aspect
@Component
@ConditionalOnClass(name = {"com.dangdang.ddframe.job.api.simple.SimpleJob"})
public class ElasticjobAop {
    public ElasticjobAop() {
    }

    @Around(value = "execution(* com.dangdang.ddframe.job.api.simple.SimpleJob.execute(..))")
    public Object arround(ProceedingJoinPoint pjp) throws Throwable {
        Transaction transaction = null;
        String name = "";
        //1、处理cat异常的情况
        try {
            Signature sig = pjp.getSignature();
            MethodSignature msig = null;
            if ((sig instanceof MethodSignature)) {
                msig = (MethodSignature) sig;
                Object target = pjp.getTarget();
                Method currentMethod = target.getClass().getMethod(msig.getName(), msig.getParameterTypes());
                //获取类名 方法名
                name = currentMethod.getDeclaringClass().getName() + "." + currentMethod.getName();
                transaction = CatUtil.initTransaction("elasticjob", name);
                TraceContext.currentInit();
                if (CatAopUtils.isPrintParams) {
                    CatUtil.logEvent("Args", name, "0", CatAopUtils.getParams(pjp));
                }
            }
        } catch (Exception e) {
            Cat.logEvent(this.getClass().getName(), e.getMessage());
        }
        Object retVal = null;
        try {
            retVal = CatAopUtils.handler(transaction, name, pjp);
        } finally {
            TraceContext.clean();
        }
        return retVal;
    }

}
