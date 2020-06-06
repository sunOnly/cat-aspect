package net.plcloud.cat.dao;


import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;

@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
@ConditionalOnClass(name = {"org.apache.ibatis.plugin.Interceptor", "com.alibaba.druid.pool.DruidDataSource"})
@Component("mybatisDiurdPluginInterceptor")
public class MybatisDiurdPluginInterceptor extends MybatisPluginInterceptor {

    private static final Logger LOGGER = Logger.getLogger(MybatisDiurdPluginInterceptor.class);

    @Override
    public String getSqlURL() {
        DataSource dataSource = super.getDataSource();
        if (dataSource == null) {
            return null;
        }
        String sqlUrl = sqlURLCache.get(dataSource.toString());
        if (sqlUrl != null) {
            return sqlUrl;
        }

        if (dataSource instanceof AbstractRoutingDataSource) {
            String methodName = "determineTargetDataSource";
            Method method = ReflectionUtils.findMethod(AbstractRoutingDataSource.class, methodName);

            if (method == null) {
                LOGGER.error(String.format("---Could not find method [%s] on target [%s]",
                        methodName, dataSource));
                return null;
            }

            ReflectionUtils.makeAccessible(method);
            DataSource dataSource1 = (DataSource) ReflectionUtils.invokeMethod(method, dataSource);
            if (dataSource1 instanceof DruidDataSource) {
                DruidDataSource druidDataSource = (DruidDataSource) dataSource1;
                sqlURLCache.put(dataSource.toString(), druidDataSource.getUrl());
                return druidDataSource.getUrl();
            } else {
                LOGGER.error("---only surpport DruidDataSource:" + dataSource1.getClass().toString());
            }
        } else if (dataSource instanceof DruidDataSource) {
            DruidDataSource druidDataSource = (DruidDataSource) dataSource;
            sqlURLCache.put(dataSource.toString(), druidDataSource.getUrl());
            return druidDataSource.getUrl();
        }
        return null;
    }

}
