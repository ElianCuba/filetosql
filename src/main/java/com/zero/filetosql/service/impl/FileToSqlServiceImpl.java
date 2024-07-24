package com.zero.filetosql.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.zero.filetosql.service.FileToSqlService;
import com.zero.filetosql.model.DbConnectionProperties;
import com.zero.filetosql.repository.SaveSQL;
import com.zero.filetosql.utils.DateUtils;
import com.zero.filetosql.utils.FieldValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class FileToSqlServiceImpl implements FileToSqlService {

    @Autowired
    private SaveSQL saveSQL;

    @Override
    public Mono<ResponseEntity<DataBuffer>> processforFile(MultipartFile file, String tableName
            , String tableCreate, String databaseType, MultipartFile fileDB, String delimiter, boolean createAutoIncrement) {

        if (file != null && !file.isEmpty() && file.getOriginalFilename() != null) {
            var exportFileName = file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf("."));
            Map<String, String> queryFinal;
            if (file.getOriginalFilename().endsWith(".csv")) {
                queryFinal = processCsv(file, tableName, tableCreate, databaseType,delimiter.trim().charAt(0),createAutoIncrement);
            } else if (file.getOriginalFilename().endsWith(".txt")) {
                queryFinal =  processTxt(file, tableName, tableCreate, databaseType,delimiter,createAutoIncrement);
            } else if (file.getOriginalFilename().endsWith(".xlsx")
                    || file.getOriginalFilename().endsWith(".xls")) {
                queryFinal = processExcel(file, tableName, tableCreate, databaseType,createAutoIncrement);
            } else if (file.getOriginalFilename().endsWith(".json")) {
                queryFinal = processJson(file, tableName, tableCreate, databaseType,createAutoIncrement);

            } else {
                return Mono.just(ResponseEntity.badRequest()
                        .body(new DefaultDataBufferFactory()
                                .wrap("Unsupported file type".getBytes())));
            }

            if (queryFinal != null && !queryFinal.isEmpty()) {
                if (fileDB != null && !fileDB.isEmpty() && fileDB.getOriginalFilename() != null) {
                    return getResponseEntityWhenCreateTableAndInsert(databaseType, fileDB, queryFinal);
                } else {
                    var createTableQuery = queryFinal.get("createTableQuery");
                    var scriptInsert = queryFinal.get("scriptInsert");
                    var sql = createTableQuery + scriptInsert;
                    return getResponseEntity(sql, exportFileName);
                }

            }
        }


        return Mono.just(ResponseEntity.badRequest()
                .body(new DefaultDataBufferFactory()
                        .wrap("No Existe el archivo".getBytes())));
    }

    private Mono<ResponseEntity<DataBuffer>> getResponseEntityWhenCreateTableAndInsert(String databaseType
            , MultipartFile fileDB
            , Map<String, String> queryFinal) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream inputStream = fileDB.getInputStream();
            // Convertir el InputStream en JsonNode
            JsonNode jsonNode = objectMapper.readTree(inputStream);

            // Acceder a los valores
            String url = jsonNode.get("url").asText();
            String username = jsonNode.get("username").asText();
            String password = jsonNode.get("password").asText();
            DbConnectionProperties dbConnectionProperties = DbConnectionProperties.builder().dbType(databaseType)
                    .url(url)
                    .username(username)
                    .password(password)
                    .build();
            var createTableQuery = queryFinal.get("createTableQuery");
            var scriptInsert = queryFinal.get("scriptInsert");

            log.info("sql: " + createTableQuery + "\n\n" + scriptInsert);

            saveSQL.createTable(dbConnectionProperties, createTableQuery);

            var totalInserted = saveSQL.batchInsert(dbConnectionProperties, scriptInsert);
            log.info("Total inserted: {}", Arrays.stream(totalInserted).count());
            return Mono.just(ResponseEntity.accepted()
                    .body(new DefaultDataBufferFactory()
                            .wrap("Se han insertado correctamente los registros en la tabla.".getBytes())));
        } catch (IOException e) {
            return Mono.error(e);
        }
    }

    private static Map<String, String> processCsv(MultipartFile file, String tableName
            , String tableCreate, String databaseType, char delimiter, boolean createAutoIncrement) {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(file.getInputStream());
            List<String[]> allRows = getAllRows(inputStreamReader,delimiter);
            return processAllRowsToScripts(allRows, tableName, tableCreate, databaseType,createAutoIncrement);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> processExcel(MultipartFile file, String tableName
            , String tableCreate, String databaseType, boolean createAutoIncrement) {
        try {
            List<String[]> allRows = readExcel(file.getInputStream(), file.getOriginalFilename());
            return processAllRowsToScripts(allRows, tableName, tableCreate, databaseType,createAutoIncrement);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Mono<ResponseEntity<DataBuffer>> getResponseEntity(String allRows, String exportFileName) {
        log.info(allRows);
        File fileResponse = new File(exportFileName + ".sql");

        return Mono.fromCallable(() -> {
            // Escribir el contenido en el archivo
            FileCopyUtils.copy(allRows.getBytes(StandardCharsets.UTF_8), fileResponse);

            // Leer el contenido del archivo
            byte[] fileContent = FileCopyUtils.copyToByteArray(fileResponse);

            // Crear el DataBuffer a partir del contenido del archivo
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);

            // Crear la respuesta
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", fileResponse.getName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(dataBuffer);
        }).onErrorMap(IOException.class, e -> new RuntimeException("Error al procesar el archivo", e));
    }

    private static List<String[]> getAllRows
            (InputStreamReader inputStreamReader, char delimiter) {
        try {
            CSVParserBuilder parserBuilder = new CSVParserBuilder();
            parserBuilder.withSeparator(delimiter);
            parserBuilder.withQuoteChar('"');

            CSVReaderBuilder readerBuilder = new CSVReaderBuilder(inputStreamReader);
            readerBuilder.withCSVParser(parserBuilder.build());
            CSVReader csvReader = readerBuilder.build();
            return csvReader.readAll();
        } catch (IOException | CsvException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> processAllRowsToScripts(List<String[]> allRows, String tableName
            , String tableCreate, String databaseType,boolean createAutoIncrement) {

        if (allRows == null || allRows.isEmpty()) {
            return null;
        }
        // Obtener los campos de la primera fila
        String[] fieldNames = allRows.getFirst();
        String[] fieldValuesValid = allRows.getLast();
        StringBuilder createTableQuery = createTableIfNotExists(tableName, fieldNames
                , fieldValuesValid, tableCreate, databaseType,createAutoIncrement);

        // Crear un mapa para asociar los nombres de los campos con sus índices
        Map<String, Integer> fieldIndices = new HashMap<>();
        for (int i = 0; i < fieldNames.length; i++) {
            fieldIndices.put(fieldNames[i], i);
        }

        // Construir el encabezado de la sentencia INSERT
        StringBuilder scriptFieldNames = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        for (String fieldName : fieldNames) {
            scriptFieldNames.append(fieldName).append(", ");
        }
        // Eliminar la última coma y espacio, y cerrar el paréntesis
        scriptFieldNames.setLength(scriptFieldNames.length() - 2);
        scriptFieldNames.append(") \n VALUES \n");

        // Construir los valores de la sentencia INSERT
        StringBuilder scriptFieldValues = new StringBuilder();
        for (int i = 1; i < allRows.size(); i++) {
            String[] fields = allRows.get(i);
            scriptFieldValues.append("(");
            for (String fieldName : fieldNames) {
                int fieldIndex = fieldIndices.get(fieldName);
                scriptFieldValues.append("'").append(fields[fieldIndex]).append("', ");
            }
            // Eliminar la última coma y espacio, y cerrar el paréntesis
            scriptFieldValues.setLength(scriptFieldValues.length() - 2);
            scriptFieldValues.append("), \n");
        }
        // Eliminar la última coma y espacio
        scriptFieldValues.setLength(scriptFieldValues.length() - 3);
        String scriptInsert = scriptFieldNames.toString() + scriptFieldValues;
        Map<String, String> queryFinal = new HashMap<>();
        queryFinal.put("createTableQuery", cleanInvisibleCharacters(createTableQuery.toString()));
        queryFinal.put("scriptInsert", cleanInvisibleCharacters(scriptInsert + ";"));

        return queryFinal;
    }

    private static StringBuilder createTableIfNotExists(String tableName
            , String[] fieldNames, String[] fieldValuesValid
            , String tableCreate, String databaseType,boolean createAutoIncrement) {
        if (!"true".equalsIgnoreCase(tableCreate)) {
            return new StringBuilder();
        }

       if (fieldNames == null || fieldNames.length == 0) {
           return new StringBuilder();
       }

        StringBuilder createTableQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldType = getFieldType(fieldValuesValid[i], databaseType);
            createTableQuery.append(fieldNames[i]).append(" ").append(fieldType).append(", \n");
        }


        if (createAutoIncrement){
            // Añadir el campo incrementable al final
            String autoIncrementType = switch (databaseType.toUpperCase()) {
                case "MARIADB", "MYSQL" -> "INT AUTO_INCREMENT";
                case "POSTGRESQL" -> "SERIAL";
                case "SQLSERVER" -> "INT IDENTITY";
                case "ORACLE" -> "NUMBER GENERATED BY DEFAULT AS IDENTITY";
                case "SQLITE" -> "INTEGER PRIMARY KEY AUTOINCREMENT";
                default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
            };
            createTableQuery.append("id ").append(autoIncrementType).append(" PRIMARY KEY, \n");
        }


        createTableQuery.setLength(createTableQuery.length() - 3); // Quitar la última coma
        createTableQuery.append(");");
        return createTableQuery;
    }

    private static String getFieldType(String fieldValue, String databaseType) {
        switch (databaseType.toUpperCase()) {
            case "MARIADB":
            case "MYSQL":
                return getMySqlFieldType(fieldValue);
            case "POSTGRESQL":
                return getPostgresFieldType(fieldValue);
            case "SQLSERVER":
                return getSqlServerFieldType(fieldValue);
            case "ORACLE":
                return getOracleFieldType(fieldValue);
            case "SQLITE":
                return getSqliteFieldType(fieldValue);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
    }

    private static String getMySqlFieldType(String fieldValue) {
        if (FieldValidator.isValidInt(fieldValue)) {
            return "INT";
        } else if (DateUtils.isDate(fieldValue)) {
            return DateUtils.isSimpleDate(fieldValue) ? "DATE" : DateUtils.isTimestamp(fieldValue) ? "DATETIME" : "VARCHAR(100)";
        } else if (FieldValidator.isValidBoolean(fieldValue)) {
            return "BOOLEAN";
        } else if (FieldValidator.isValidDouble(fieldValue)) {
            return "DECIMAL(10, 2)";
        } else if (FieldValidator.isValidText(fieldValue)) {
            return "VARCHAR(100)";
        }
        return "VARCHAR(100)";
    }

    private static String getPostgresFieldType(String fieldValue) {
        if (FieldValidator.isValidInt(fieldValue)) {
            return "INTEGER";
        } else if (DateUtils.isDate(fieldValue)) {
            return DateUtils.isSimpleDate(fieldValue) ? "DATE" : DateUtils.isTimestamp(fieldValue) ? "TIMESTAMP" : "VARCHAR(100)";
        } else if (FieldValidator.isValidBoolean(fieldValue)) {
            return "BOOLEAN";
        } else if (FieldValidator.isValidDouble(fieldValue)) {
            return "NUMERIC(10, 2)";
        } else if (FieldValidator.isValidText(fieldValue)) {
            return "VARCHAR(100)";
        }
        return "VARCHAR(100)";
    }

    private static String getSqlServerFieldType(String fieldValue) {
        if (FieldValidator.isValidInt(fieldValue)) {
            return "INT";
        } else if (DateUtils.isDate(fieldValue)) {
            return DateUtils.isSimpleDate(fieldValue) ? "DATE" : DateUtils.isTimestamp(fieldValue) ? "DATETIME" : "VARCHAR2(100)";
        } else if (FieldValidator.isValidBoolean(fieldValue)) {
            return "BOOLEAN";
        } else if (FieldValidator.isValidDouble(fieldValue)) {
            return "DECIMAL(10, 2)";
        } else if (FieldValidator.isValidText(fieldValue)) {
            return "VARCHAR2(100)";
        }
        return "VARCHAR2(100)";
    }

    private static String getOracleFieldType(String fieldValue) {
        if (FieldValidator.isValidInt(fieldValue)) {
            return "NUMBER";
        } else if (DateUtils.isDate(fieldValue)) {
            return DateUtils.isSimpleDate(fieldValue) ? "DATE" : DateUtils.isTimestamp(fieldValue) ? "DATETIME" : "VARCHAR2(100)";
        } else if (FieldValidator.isValidBoolean(fieldValue)) {
            return "BOOLEAN";
        } else if (FieldValidator.isValidDouble(fieldValue)) {
            return "DECIMAL(10, 2)";
        } else if (FieldValidator.isValidText(fieldValue)) {
            return "VARCHAR2(100)";
        }
        return "VARCHAR2(100)";
    }

    private static String getSqliteFieldType(String fieldValue) {
        if (FieldValidator.isValidInt(fieldValue)) {
            return "INTEGER";
        } else if (DateUtils.isDate(fieldValue)) {
            return "TEXT";
        } else if (FieldValidator.isValidText(fieldValue)) {
            return "TEXT(100)";
        }
        return "TEXT(100)";
    }


    private Map<String, String> processTxt(MultipartFile file, String tableName
            , String tableCreate, String databaseType, String delimiter, boolean createAutoIncrement) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
            List<String[]> allRows = new ArrayList<>();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] value = linea.split(delimiter);
                allRows.add(value);
            }
            if (!allRows.isEmpty()) {
                return processAllRowsToScripts(allRows, tableName, tableCreate, databaseType,createAutoIncrement);

            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String[]> readExcel(InputStream inputStream, String originalFilename) {
        List<String[]> data = new ArrayList<>();
        try (Workbook workbook = getWorkbook(inputStream, originalFilename)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    rowData.add(getCellValue(cell));
                }
                data.add(rowData.toArray(new String[0]));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading Excel file", e);
        }
        return data;
    }

    private static Workbook getWorkbook(InputStream inputStream, String originalFilename) throws IOException {
        if (originalFilename.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (originalFilename.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        } else {
            throw new IllegalArgumentException("El archivo no es un Excel válido.");
        }
    }

    private static String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return formatStringCell(cell.getStringCellValue());
            case NUMERIC:
                return formatNumericCell(cell);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "Valor no soportado";
        }
    }

    private static String formatStringCell(String cellValue) {
        if (DateUtils.isDate(cellValue)) {
            log.info("DATE: ");
            return DateUtils.formatDate(cellValue);
        } else if (DateUtils.isTimestamp(cellValue)) {
            log.info("TIMESTAMP: ");
            return DateUtils.formatTimestamp(cellValue);
        } else {
            return cellValue;
        }
    }

    private static String formatNumericCell(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return "DATE: " + cell.getDateCellValue().toString();
        } else {
            return String.valueOf(cell.getNumericCellValue());
        }
    }

    private Map<String, String> processJson(MultipartFile file, String tableName, String tableCreate, String databaseType, boolean createAutoIncrement) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream inputStream = file.getInputStream();
            List<Map<String, Object>> dataList = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            return generateSQLStatements(dataList, tableName, tableCreate, databaseType, createAutoIncrement);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> generateSQLStatements(List<Map<String, Object>> dataList, String tableName
            , String tableCreate, String databaseType, boolean createAutoIncrement) {
        Map<String, Object> firstEntry = dataList.getFirst();
        Set<String> fieldNamesSet = firstEntry.keySet();
        String[] fieldNames = fieldNamesSet.toArray(new String[0]);

        String[] fieldValuesValid = firstEntry.values().stream()
                .map(Object::toString)
                .toArray(String[]::new);
        StringBuilder createTableQuery = createTableIfNotExists(tableName, fieldNames
                , fieldValuesValid, tableCreate, databaseType,createAutoIncrement);

        // Construir el encabezado de la sentencia INSERT
        StringBuilder scriptFieldNames = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        for (String fieldName : fieldNames) {
            scriptFieldNames.append(fieldName).append(", ");
        }
        // Eliminar la última coma y espacio, y cerrar el paréntesis
        scriptFieldNames.setLength(scriptFieldNames.length() - 2);
        scriptFieldNames.append(") \n VALUES \n");


        String columns = firstEntry.keySet().stream()
                .collect(Collectors.joining(", "));

        // Construir los valores de la sentencia INSERT
        String values = dataList.stream()
                .map(row -> row.values().stream()
                        .map(this::formatValue)
                        .collect(Collectors.joining(", ", "(", ")"))
                )
                .collect(Collectors.joining(", "));

        var sql = String.format("INSERT INTO %s (%s) VALUES %s;", tableName, columns, values);

        log.info("SQL statements: " + sql);
        Map<String, String> queryFinal = new HashMap<>();
        queryFinal.put("createTableQuery",cleanInvisibleCharacters(createTableQuery.toString()));
        queryFinal.put("scriptInsert", cleanInvisibleCharacters(sql));


        return queryFinal;
    }

    private String formatValue(Object value) {
        if (value instanceof String) {
            // Escapar adecuadamente las comillas si es necesario
            return String.format("'%s'", value);
        } else if (value instanceof Boolean) {
            return (Boolean) value ? "TRUE" : "FALSE";
        } else {
            return value.toString();
        }
    }

    private static String cleanInvisibleCharacters(String input) {
        // Expresión regular para eliminar caracteres invisibles
        return input.replaceAll("\\uFEFF|\\u200C|\\u200D|\\u200B", "");
    }
}
