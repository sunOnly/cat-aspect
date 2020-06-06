package net.plcloud.cat.dao;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.annotation.Resource;
import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(name = {"org.apache.ibatis.plugin.Interceptor", "org.apache.ibatis.session.SqlSessionFactory"})
public class MyBatisConfig {

    @Resource
    private MybatisPluginInterceptor mybatisPluginInterceptor;

    @Resource
    private DataSource dataSource;

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean fb = new SqlSessionFactoryBean();
        try {
            fb.setPlugins(new Interceptor[]{mybatisPluginInterceptor});
            fb.setDataSource(dataSource);
            fb.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:**/*Mapper.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fb.getObject();
    }

}
