package com.namejm.stream_guppy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfig {

    @Value("${stream_guppy.database:h2}")
    private String databaseType;

    @Value("${spring.datasource.url}")
    private String defaultUrl;

    @Value("${spring.datasource.username}")
    private String defaultUsername;

    @Value("${spring.datasource.password}")
    private String defaultPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String defaultDriverClassName;

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String hibernateDdlAuto;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Value("${spring.jpa.properties.hibernate.format_sql:false}")
    private boolean formatSql;

    // Database-specific properties with default values
    @Value("${stream_guppy.datasource.oracle.url:#{null}}")
    private String oracleUrl;

    @Value("${stream_guppy.datasource.oracle.username:#{null}}")
    private String oracleUsername;

    @Value("${stream_guppy.datasource.oracle.password:#{null}}")
    private String oraclePassword;

    @Value("${stream_guppy.datasource.oracle.driver-class-name:oracle.jdbc.OracleDriver}")
    private String oracleDriverClassName;

    @Value("${stream_guppy.datasource.mssql.url:#{null}}")
    private String mssqlUrl;

    @Value("${stream_guppy.datasource.mssql.username:#{null}}")
    private String mssqlUsername;

    @Value("${stream_guppy.datasource.mssql.password:#{null}}")
    private String mssqlPassword;

    @Value("${stream_guppy.datasource.mssql.driver-class-name:com.microsoft.sqlserver.jdbc.SQLServerDriver}")
    private String mssqlDriverClassName;

    @Value("${stream_guppy.datasource.postgresql.url:#{null}}")
    private String postgresqlUrl;

    @Value("${stream_guppy.datasource.postgresql.username:#{null}}")
    private String postgresqlUsername;

    @Value("${stream_guppy.datasource.postgresql.password:#{null}}")
    private String postgresqlPassword;

    @Value("${stream_guppy.datasource.postgresql.driver-class-name:org.postgresql.Driver}")
    private String postgresqlDriverClassName;

    @Value("${stream_guppy.datasource.mysql.url:#{null}}")
    private String mysqlUrl;

    @Value("${stream_guppy.datasource.mysql.username:#{null}}")
    private String mysqlUsername;

    @Value("${stream_guppy.datasource.mysql.password:#{null}}")
    private String mysqlPassword;

    @Value("${stream_guppy.datasource.mysql.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String mysqlDriverClassName;

    @Value("${stream_guppy.datasource.mariadb.url:#{null}}")
    private String mariadbUrl;

    @Value("${stream_guppy.datasource.mariadb.username:#{null}}")
    private String mariadbUsername;

    @Value("${stream_guppy.datasource.mariadb.password:#{null}}")
    private String mariadbPassword;

    @Value("${stream_guppy.datasource.mariadb.driver-class-name:org.mariadb.jdbc.Driver}")
    private String mariadbDriverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

        switch (databaseType.toLowerCase()) {
            case "oracle":
                dataSourceBuilder.driverClassName(oracleDriverClassName);
                dataSourceBuilder.url(oracleUrl);
                dataSourceBuilder.username(oracleUsername);
                dataSourceBuilder.password(oraclePassword);
                break;
            case "mssql":
                dataSourceBuilder.driverClassName(mssqlDriverClassName);
                dataSourceBuilder.url(mssqlUrl);
                dataSourceBuilder.username(mssqlUsername);
                dataSourceBuilder.password(mssqlPassword);
                break;
            case "postgresql":
                dataSourceBuilder.driverClassName(postgresqlDriverClassName);
                dataSourceBuilder.url(postgresqlUrl);
                dataSourceBuilder.username(postgresqlUsername);
                dataSourceBuilder.password(postgresqlPassword);
                break;
            case "mysql":
                dataSourceBuilder.driverClassName(mysqlDriverClassName);
                dataSourceBuilder.url(mysqlUrl);
                dataSourceBuilder.username(mysqlUsername);
                dataSourceBuilder.password(mysqlPassword);
                break;
            case "mariadb":
                dataSourceBuilder.driverClassName(mariadbDriverClassName);
                dataSourceBuilder.url(mariadbUrl);
                dataSourceBuilder.username(mariadbUsername);
                dataSourceBuilder.password(mariadbPassword);
                break;
            case "h2":
            default:
                dataSourceBuilder.driverClassName(defaultDriverClassName);
                dataSourceBuilder.url(defaultUrl);
                dataSourceBuilder.username(defaultUsername);
                dataSourceBuilder.password(defaultPassword);
                break;
        }

        return dataSourceBuilder.build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.namejm.stream_guppy.vo");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();

        // Set dialect based on database type
        switch (databaseType.toLowerCase()) {
            case "oracle":
                properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
                break;
            case "mssql":
                properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
                break;
            case "postgresql":
                properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
                break;
            case "mysql":
                properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
                break;
            case "mariadb":
                properties.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
                break;
            case "h2":
            default:
                properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                break;
        }


        properties.put("hibernate.hbm2ddl.auto", hibernateDdlAuto);
        properties.put("hibernate.show_sql", showSql);
        properties.put("hibernate.format_sql", formatSql);

        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
