package net.plcloud.cat.util;

import com.alibaba.fastjson.JSON;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import org.aspectj.lang.ProceedingJoinPoint;

public class CatAopUtils {
    public static boolean isPrintParams = "true".equals(System.getProperty("cat.printParams"));
    public static boolean isPrintResult = "true".equals(System.getProperty("cat.printResult"));

    /**
     * 获取调用参数
     *
     * @param pjp
     * @return
     */
    public static String getParams(ProceedingJoinPoint pjp) {
        String message = "";
        try {
            StringBuilder sbf = new StringBuilder();
            for (Object o : pjp.getArgs()) {
                if (o != null) {
                    sbf.append(",").append(JSON.toJSONString(o));
                }
            }
            if (sbf.length() > 1) {
                message += "(" + sbf.toString().substring(1) + ")";
            }
        } catch (Exception e) {
            Cat.getProducer().logEvent("CatAopUtils", e.getMessage());
        }
        return message;
    }

    public static Object handler(Transaction transaction, String name, ProceedingJoinPoint pjp) throws Throwable {
        //2、调用核心逻辑
        Object retVal = null;
        Throwable ex = null;
        try {
            retVal = pjp.proceed();
        } catch (Throwable e) {
            ex = e;
            throw e;
        } finally {
            try {
                if (ex != null) {
                    if (!CatAopUtils.isPrintParams) {
                        CatUtil.logEvent("Args", name, "0", CatAopUtils.getParams(pjp));
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
}