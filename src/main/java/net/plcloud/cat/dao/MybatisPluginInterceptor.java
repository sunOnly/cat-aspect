package net.plcloud.cat.dao;


import com.alibaba.fastjson.JSON;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import net.plcloud.cat.util.CatAopUtils;
import net.plcloud.cat.util.CatUtil;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.log4j.Logger;
import org.mybatis.spring.transaction.SpringManagedTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

@ConditionalOnClass(name = {"org.apache.ibatis.plugin.Interceptor"})
public abstract class MybatisPluginInterceptor implements Interceptor {

    private static final Logger LOGGER = Logger.getLogger(MybatisPluginInterceptor.class);
    //缓存，提高性能
    static final Map<String, String> sqlURLCache = new ConcurrentHashMap<String, String>(256);

    private Executor target;

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            this.target = (Executor) target;
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Transaction transaction = null;
        String methodName = null;
        Object parameter = null;
        BoundSql boundSql = null;
        Configuration configuration = null;
        try {
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            //得到类名，方法
            String[] strArr = mappedStatement.getId().split("\\.");
            methodName = strArr[strArr.length - 2] + "." + strArr[strArr.length - 1];
            transaction = CatUtil.initTransaction("SQL", methodName);
            //得到sql语句
            if (invocation.getArgs().length > 1) {
                parameter = invocation.getArgs()[1];
            }
            boundSql = mappedStatement.getBoundSql(parameter);
            configuration = mappedStatement.getConfiguration();
//            showSql(configuration, boundSql, transaction);
            String sql = showSql(configuration, boundSql);
            transaction.addData(sql);

            String s = this.getSQLDatabase();
            CatUtil.logEvent("SQL.Database", s);
        } catch (Exception e) {
            LOGGER.error(e);
        }


        Object returnObj = null;
        Throwable ex = null;
        try {
            returnObj = invocation.proceed();
        } catch (Exception e) {
            ex = e;
            throw e;
        } finally {
            try {
                if (ex != null) {
                    if (!CatAopUtils.isPrintParams) {
                        CatUtil.logEvent("SQL.Method", methodName, Message.SUCCESS, showSql(configuration, boundSql));
                    }
                    CatUtil.errorCat(transaction, ex);
                } else {
                    if (CatAopUtils.isPrintResult) {
                        Cat.logEvent("Result", methodName, "0", JSON.toJSONString(returnObj));
                    }
                    CatUtil.successCat(transaction);
                }
            } catch (Throwable e) {
                CatUtil.successCat(transaction, methodName, e);
            }
        }
        return returnObj;
    }

    javax.sql.DataSource getDataSource() {
        org.apache.ibatis.transaction.Transaction transaction = this.target.getTransaction();
        if (transaction == null) {
            LOGGER.error(String.format("Could not find transaction on target [%s]", this.target));
            return null;
        }
        if (transaction instanceof SpringManagedTransaction) {
            String fieldName = "dataSource";
            Field field = ReflectionUtils.findField(transaction.getClass(), fieldName, javax.sql.DataSource.class);

            if (field == null) {
                LOGGER.error(String.format("Could not find field [%s] of type [%s] on target [%s]",
                        fieldName, javax.sql.DataSource.class, this.target));
                return null;
            }

            ReflectionUtils.makeAccessible(field);
            javax.sql.DataSource dataSource = (javax.sql.DataSource) ReflectionUtils.getField(field, transaction);
            return dataSource;
        }

        LOGGER.error(String.format("---the transaction is not SpringManagedTransaction:%s", transaction.getClass().toString()));

        return null;
    }


    private String getTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "." + calendar.get(Calendar.MILLISECOND);
    }

    private boolean isRecordtime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        if (calendar.get(Calendar.MINUTE) % 15 == 0 && calendar.get(Calendar.SECOND) % 50 == 0) {
            return true;
        }
        return false;
    }

    public abstract String getSqlURL();

    private String getSQLDatabase() {
        return this.getSqlURL();
    }

    /**
     * 解析sql语句
     *
     * @param configuration
     * @param boundSql
     * @return
     */
    private String showSql(Configuration configuration, BoundSql boundSql, Transaction transaction) {
        Object parameterObject = boundSql.getParameterObject();
        AtomicInteger num = new AtomicInteger(0);
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        transaction.addData(sql);
        if (parameterMappings.size() > 0 && parameterObject != null && CatAopUtils.isPrintParams) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                transaction.addData(String.valueOf(num.getAndIncrement()), Matcher.quoteReplacement(getParameterValue(parameterObject)));
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        transaction.addData(String.valueOf(num.getAndIncrement()), Matcher.quoteReplacement(getParameterValue(obj)));

                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        transaction.addData(String.valueOf(num.getAndIncrement()), Matcher.quoteReplacement(getParameterValue(obj)));
                    }
                }
            }
        }
        return sql;
    }

    private String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null && CatAopUtils.isPrintParams) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));

            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    }
                }
            }
        }
        return sql;
    }

    /**
     * 参数解析
     *
     * @param obj
     * @return
     */
    private String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format((Date) obj) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        return value;
    }

}
