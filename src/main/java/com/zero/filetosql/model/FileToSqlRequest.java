package com.zero.filetosql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class FileToSqlRequest {
    private String fileName;
    private String fileType;
    private String path;
    private String separator;

    private String type;
    private String conectionString;

    private String dbJurl;
    private String dbUser;
    private String dbPassword;
    private String tableName;
    private String tableCreate;
}
