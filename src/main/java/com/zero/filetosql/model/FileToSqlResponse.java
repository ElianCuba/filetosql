package com.zero.filetosql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class FileToSqlResponse {
    private String fileName;
    private String urlDownload;
    private String countRegister;

}
