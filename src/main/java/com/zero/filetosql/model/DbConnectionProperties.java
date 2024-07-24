package com.zero.filetosql.model;

import lombok.*;

@Getter
@Builder(builderClassName = "Builder")
@AllArgsConstructor
@ToString(exclude = "password")
public final class DbConnectionProperties {
    private final String dbType;
    private final String url;
    private final String username;
    private final String password;
}