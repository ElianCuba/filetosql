package com.zero.filetosql.config;

import com.zero.filetosql.model.DbConnectionProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DynamicDataSourceProvider {

    private static final Map<String, String> DRIVER_MAP = new HashMap<>();

    static {
        DRIVER_MAP.put("postgresql", "org.postgresql.Driver");
        DRIVER_MAP.put("mysql", "com.mysql.cj.jdbc.Driver");
        DRIVER_MAP.put("mariadb", "org.mariadb.jdbc.Driver");
        DRIVER_MAP.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DRIVER_MAP.put("oracle", "oracle.jdbc.OracleDriver");
        DRIVER_MAP.put("sqlite", "org.sqlite.JDBC");
        // Añade más mappings para otros tipos de bases de datos según sea necesario
    }


    public JdbcTemplate createJdbcTemplate(DbConnectionProperties dbConnectionProperties) {
        String driverClassName = DRIVER_MAP.get(dbConnectionProperties.getDbType().toLowerCase());
        if (driverClassName == null) {
            throw new IllegalArgumentException("Unsupported database type: " + dbConnectionProperties.getDbType());
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(dbConnectionProperties.getUrl());
        dataSource.setUsername(dbConnectionProperties.getUsername());
        dataSource.setPassword(dbConnectionProperties.getPassword());

        return new JdbcTemplate(dataSource);
    }
}

