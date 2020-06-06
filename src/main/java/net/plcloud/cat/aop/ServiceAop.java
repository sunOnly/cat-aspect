package net.plcloud.cat.aop;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import net.plcloud.cat.util.CatAopUtils;
import net.plcloud.cat.util.CatUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/***
 * 业务层监控
 * @author xuhaibo
 *
 */
@Aspect
@Component
public class ServiceAop {

    @Pointcut(value = "execution(* com.*..*.service.impl..*ServiceImpl.*(..))")
    public void access() {
    }

    @Around("access()")
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
                name = currentMethod.getDeclaringClass().getName() + "." + currentMethod.getName();
                transaction = CatUtil.initTransaction("Service", name);
                if (CatAopUtils.isPrintParams) {
                    CatUtil.logEvent("Args", name, "0", CatAopUtils.getParams(pjp));
                }
            }
        } catch (Exception e) {
            Cat.logEvent(this.getClass().getName(),e.getMessage());
        }

        //2、调用核心逻辑
        return CatAopUtils.handler(transaction, name, pjp);
    }
}
