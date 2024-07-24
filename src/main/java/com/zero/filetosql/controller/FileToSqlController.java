package com.zero.filetosql.controller;

import com.zero.filetosql.service.FileToSqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@CrossOrigin
@RestController
@RequestMapping("/api/v1")
public class FileToSqlController {

    @Autowired
    private FileToSqlService fileToSqlService;

    @PostMapping("/fileToTable")
    @ResponseStatus(HttpStatus.CREATED)
    public  Mono<ResponseEntity<DataBuffer>> importFileToTable(@RequestParam("file") MultipartFile file
            , @RequestParam("tableName") String tableName
            , @RequestParam("tableCreate") String tableCreate
            , @RequestParam("databaseType") String databaseType
            , @RequestParam("fileDB") MultipartFile fileDB
            , @RequestParam("delimiter") String delimiter
            , @RequestParam("createAutoIncrement") boolean createAutoIncrement) {

        return fileToSqlService.processforFile(file,tableName,tableCreate,databaseType,fileDB, delimiter, createAutoIncrement);
    }

    @PostMapping("/fileToSQL")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<DataBuffer>> importFileToSQL(@RequestParam("file") MultipartFile file
            , @RequestParam("tableName") String tableName
            , @RequestParam("tableCreate") String tableCreate
            , @RequestParam("databaseType") String databaseType
            , @RequestParam("delimiter") String delimiter
            , @RequestParam("createAutoIncrement") boolean createAutoIncrement) {
        return fileToSqlService.processforFile(file,tableName,tableCreate,databaseType,null,delimiter, createAutoIncrement);
    }

}
