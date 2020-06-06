package net.plcloud.cat.util;

import com.dianping.cat.Cat;
import com.dianping.cat.exception.BussinessException;
import com.dianping.cat.exception.RuntimeBussinessException;
import com.dianping.cat.message.Transaction;
import net.plcloud.cat.dubbo.constants.CatConstants;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

@Component
@Order(value = 1)
@ConfigurationProperties(prefix = "cat")
public class CatUtil implements ApplicationRunner {
    private String appName;
    private String printParams;
    private String printResult;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPrintParams() {
        return printParams;
    }

    public void setPrintParams(String printParams) {
        this.printParams = printParams;
    }

    public String getPrintResult() {
        return printResult;
    }

    public void setPrintResult(String printResult) {
        this.printResult = printResult;
    }

    /***
     * 初始化cat监控
     */
    @Override
    public void run(ApplicationArguments arg0) throws Exception {
        if (System.getProperty("cat.appName") == null && this.getAppName() != null && this.getAppName().length() > 0) {
            System.setProperty("cat.appName", getAppName());
        }
        if (System.getProperty("cat.printParams") == null && this.getPrintParams() != null) {
            System.setProperty("cat.printParams", getPrintParams());
        }

        if (System.getProperty("cat.printResult") == null && this.getPrintResult() != null) {
            System.setProperty("cat.printResult", getPrintResult());
        }

        Cat.getProducer();
    }

    /**
     * 初始化监控事务
     *
     * @param type
     * @param content
     * @return
     */
    public static Transaction initTransaction(String type, String content) {
        Transaction t = null;
        try {
            t = Cat.getProducer().newTransaction(type, content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    /***
     * 出错处理 带错误说明
     * @param t
     * @param message
     * @param e
     */
    public static void errorCat(Transaction t, String message, Throwable e) {
        try {
            if (t != null) {
                if (isBussinessException(e)) {
                    t.setStatus("0");
                } else {
                    t.setStatus(e);
                }
                Cat.getProducer().logError(message, e);
                t.complete();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    /***
     * 出错处理 不带错误说明
     * @param t
     * @param e
     */
    public static void errorCat(Transaction t, Throwable e) {
        try {
            if (t != null) {
                t.setStatus(e);
                Cat.getProducer().logError(e);
                t.complete();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    /***
     * 出错处理 不带错误说明
     * @param t
     * @param e
     */
    public static void errorCat(Transaction t, Exception e) {
        try {
            if (t != null) {
                t.setStatus(e);
                Cat.getProducer().logError(e);
                t.complete();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    /***
     * 正常处理
     * @param t
     */
    public static void successCat(Transaction t) {
        try {
            if (t != null) {
                t.setStatus(Transaction.SUCCESS);
                t.complete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void successCat(Transaction t, Throwable ex) {
        successCat(t, "", ex);
    }

    /***
     * 正常处理
     * @param t
     * @param ex
     */
    public static void successCat(Transaction t, String loggerName, Throwable ex) {
        try {
            if (t != null) {
                StringWriter writer = new StringWriter(2048);
                ex.printStackTrace(new PrintWriter(writer));
                String detailMessage = writer.toString();
                Cat.logEvent(CatConstants.BussinessException, loggerName, "0", detailMessage);
                t.setStatus(Transaction.SUCCESS);
                t.complete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logEvent(String type, String content) {
        Cat.logEvent(type, content);
    }

    public static void logEvent(String type, String name, String status, String nameValuePairs) {
        Cat.logEvent(type, name, status, nameValuePairs);
    }

    public static boolean isBussinessException(Throwable throwable) {
        if (throwable instanceof RuntimeBussinessException || throwable instanceof BussinessException) {
            return true;
        }
        if (throwable.getMessage() != null && (throwable.getMessage().startsWith(RuntimeBussinessException.class.getName()) || throwable.getMessage().startsWith(BussinessException.class.getName()))) {
            return true;
        }
        return false;
    }
}
