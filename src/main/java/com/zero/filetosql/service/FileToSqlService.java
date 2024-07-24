package com.zero.filetosql.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

public interface FileToSqlService {
    Mono<ResponseEntity<DataBuffer>>  processforFile(MultipartFile file, String tebleName
            , String tableCreate, String databaseType, MultipartFile fileDB, String delimiter,boolean createAutoIncrement);
}
