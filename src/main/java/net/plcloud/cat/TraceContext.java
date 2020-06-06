package net.plcloud.cat;

import com.dianping.cat.Cat;
import com.dianping.cat.CatConstants;
import com.dianping.cat.message.Event;
import com.dianping.cat.message.spi.MessageTree;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

public class TraceContext {

    private static boolean isLog4j2 = ClassUtils.isPresent("org.apache.logging.log4j.Logger", TraceContext.class.getClassLoader());
    private static boolean isLogback = ClassUtils.isPresent("ch.qos.logback.core.AppenderBase", TraceContext.class.getClassLoader());
    private static boolean isLog4j = ClassUtils.isPresent("org.apache.log4j.Logger", TraceContext.class.getClassLoader());

    public static String getRootMessageId() {
        MessageTree tree = Cat.getManager().getThreadLocalMessageTree();
        String messageId = tree.getMessageId();

        if (messageId == null) {
            messageId = Cat.createMessageId();
            tree.setMessageId(messageId);
        }

        String root = tree.getRootMessageId();

        if (root == null) {
            root = messageId;
        }
        return root;
    }

    public static void init() {
        init(getRootMessageId());
    }

    public static void currentInit() {
        init(Cat.getCurrentMessageId());
    }

    public static void clean() {
        if (isLog4j2) {
            org.apache.logging.log4j.ThreadContext.remove("catTraceId");
        }
        if (isLogback) {
            org.slf4j.MDC.remove("catTraceId");
        }
        if (isLog4j) {
            org.apache.log4j.MDC.remove("catTraceId");
        }
    }

    public static void init(String traceId) {
        if (traceId == null) {
            traceId = getRootMessageId();
        }
        if (isLog4j2) {
            org.apache.logging.log4j.ThreadContext.put("catTraceId", traceId);
        }
        if (isLogback) {
            org.slf4j.MDC.put("catTraceId", traceId);
        }
        if (isLog4j) {
            org.apache.log4j.MDC.put("catTraceId", traceId);
        }
    }

    public static boolean isInit() {
        Object catTraceId = null;
        if (isLog4j2) {
            catTraceId = org.apache.logging.log4j.ThreadContext.get("catTraceId");
        }
        if (isLogback && catTraceId == null) {
            catTraceId = org.slf4j.MDC.get("catTraceId");
        }
        if (isLog4j && catTraceId == null) {
            catTraceId = org.apache.log4j.MDC.get("catTraceId");
        }
        return catTraceId != null;
    }
}
