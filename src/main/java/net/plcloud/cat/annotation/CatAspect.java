package net.plcloud.cat.annotation;

import com.dianping.cat.Cat;
import com.dianping.cat.exception.BussinessException;
import com.dianping.cat.exception.RuntimeBussinessException;
import com.dianping.cat.message.Transaction;
import com.google.common.base.Strings;
import net.plcloud.cat.util.CatUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class CatAspect {
    public CatAspect() {
    }

    @Around("@annotation(catTransaction)")
    public Object catTransactionProcess(ProceedingJoinPoint pjp, CatTransaction catTransaction) throws Throwable {
        String transName = pjp.getSignature().getDeclaringType().getSimpleName() + "." + pjp.getSignature().getName();
        if (Strings.isNullOrEmpty(transName)) {
            transName = catTransaction.name();
        }
        Transaction t = CatUtil.initTransaction(catTransaction.type(), transName);
        try {
            Object result = pjp.proceed();
            return result;
        } catch (BussinessException be) {
            CatUtil.successCat(t, be);
            throw be;
        } catch (RuntimeBussinessException rbe) {
            CatUtil.successCat(t, rbe);
            throw rbe;
        } catch (Throwable e) {
            t.setStatus(e);
            throw e;
        } finally {
            t.complete();
        }
    }

    @Around("@annotation(catCacheTransaction)")
    public Object catCacheTransactionProcess(ProceedingJoinPoint pjp, CatCacheTransaction catCacheTransaction) throws Throwable {
        String transName = pjp.getSignature().getName();
        if (Strings.isNullOrEmpty(transName)) {
            transName = catCacheTransaction.name();
        }
        Transaction t = Cat.newTransaction("Cache.Redis", transName);
        try {
            Cat.logEvent("Cache.Server", catCacheTransaction.server());
            Object result = pjp.proceed();
            t.setStatus(Transaction.SUCCESS);
            return result;
        } catch (BussinessException be) {
            CatUtil.successCat(t, be);
            throw be;
        } catch (RuntimeBussinessException rbe) {
            CatUtil.successCat(t, rbe);
            throw rbe;
        } catch (Throwable e) {
            t.setStatus(e);
            throw e;
        } finally {
            t.complete();
        }
    }

    @Around("@annotation(catMqTransaction)")
    public Object catMqTransactionProcess(ProceedingJoinPoint pjp, CatMqTransaction catMqTransaction) throws Throwable {
        String transName = pjp.getSignature().getName();
        if (Strings.isNullOrEmpty(transName)) {
            transName = catMqTransaction.name();
        }
        Transaction t = Cat.newTransaction("MQ.Rocket", transName);
        try {
            Cat.logEvent("MQ.Server", catMqTransaction.server());
            Object result = pjp.proceed();
            t.setStatus(Transaction.SUCCESS);
            return result;
        }catch (BussinessException be) {
            CatUtil.successCat(t, be);
            throw be;
        } catch (RuntimeBussinessException rbe) {
            CatUtil.successCat(t, rbe);
            throw rbe;
        }  catch (Throwable e) {
            t.setStatus(e);
            throw e;
        } finally {
            t.complete();
        }
    }

    @Around("@annotation(catDubboClientTransaction)")
    public Object catDubboServerTransactionProcess(ProceedingJoinPoint pjp, CatDubboClientTransaction catDubboClientTransaction) throws Throwable {
        String transName = pjp.getSignature().getName();
        if (Strings.isNullOrEmpty(transName)) {
            transName = catDubboClientTransaction.name();
        }
        Transaction t = Cat.newTransaction("Call", transName);
        try {
            Cat.logEvent("Call.app", catDubboClientTransaction.callApp());
            Cat.logEvent("Call.server", catDubboClientTransaction.callServer());
            Object result = pjp.proceed();
            t.setStatus(Transaction.SUCCESS);
            return result;
        } catch (BussinessException be) {
            CatUtil.successCat(t, be);
            throw be;
        } catch (RuntimeBussinessException rbe) {
            CatUtil.successCat(t, rbe);
            throw rbe;
        } catch (Throwable e) {
            t.setStatus(e);
            throw e;
        } finally {
            t.complete();
        }
    }

}