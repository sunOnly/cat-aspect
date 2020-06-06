package net.plcloud.cat.log;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogbackAppenderTest {
    /**
     * @param args
     */
    public static void main(String[] args) {
        System.setProperty("cat.appName", "logback");
        int a = 0;
        try {
            System.out.println(1 / a);
        } catch (Exception e) {
            log.error("error", e);
        }
        try {
            Thread.sleep(1000 * 60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
