package net.plcloud.cat.log;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Log4j2AppenderTest {
    static Logger logger = LogManager.getLogger(Log4j2AppenderTest.class.getName());

    public static boolean hello() {
        logger.trace("entry");//等同于logger.entry();但此方法在新版本好像已经废弃
        logger.error("Did it again!");
        logger.info("这是info级信息");
        logger.debug("这是debug级信息");
        logger.warn("这是warn级信息");
        logger.fatal("严重错误");
        logger.trace("exit");
        return false;
    }


    public static void main(String[] args) {
        System.setProperty("cat.appName", "log4j2");
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