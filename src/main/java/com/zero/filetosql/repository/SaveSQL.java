package com.zero.filetosql.repository;


import com.zero.filetosql.config.DynamicDataSourceProvider;
import com.zero.filetosql.model.DbConnectionProperties;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class SaveSQL  {

    @Autowired
    private DynamicDataSourceProvider dynamicDataSourceProvider;

    public void createTable(DbConnectionProperties dbConnectionProperties, String query) {
        JdbcTemplate jdbcTemplate = dynamicDataSourceProvider.createJdbcTemplate(dbConnectionProperties);
        jdbcTemplate.update(query);
    }

    public int[] batchInsert(DbConnectionProperties dbConnectionProperties, String query) {
        JdbcTemplate jdbcTemplate = dynamicDataSourceProvider.createJdbcTemplate(dbConnectionProperties);
        return jdbcTemplate.batchUpdate(query);
    }
}
