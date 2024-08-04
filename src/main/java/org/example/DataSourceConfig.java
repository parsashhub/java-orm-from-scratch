package org.example;

import org.apache.commons.dbcp2.BasicDataSource;

public class DataSourceConfig {

    public static BasicDataSource createDataSource() {

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("123456");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // configurations for pool size, timeouts, etc.
        dataSource.setInitialSize(5);
        dataSource.setMaxIdle(10);
        dataSource.setMaxTotal(20);

        return dataSource;
    }
}
