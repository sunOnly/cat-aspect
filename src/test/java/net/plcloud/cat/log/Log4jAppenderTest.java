package net.plcloud.cat.log;

import org.apache.log4j.Logger;

public class Log4jAppenderTest {
    private static Logger logger = Logger.getLogger(Log4jAppenderTest.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.setProperty("cat.appName", "log4j");
        int a = 0;
        try {
            System.out.println(1 / a);
        } catch (Exception e) {
            logger.error("error", e);
        }
        try {
            Thread.sleep(1000 * 60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
