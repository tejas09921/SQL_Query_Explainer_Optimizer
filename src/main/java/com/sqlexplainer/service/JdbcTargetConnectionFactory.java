package com.sqlexplainer.service;

import com.sqlexplainer.dto.ConnectionRequest;
import com.sqlexplainer.exception.TargetDbException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class JdbcTargetConnectionFactory {

    public Connection open(ConnectionRequest request) {
        String sslMode = request.sslMode() == null || request.sslMode().isBlank() ? "disable" : request.sslMode();
        String url = "jdbc:postgresql://" + request.host() + ":" + request.port() + "/" + request.database()
                + "?sslmode=" + sslMode;
        Properties properties = new Properties();
        properties.setProperty("user", request.username());
        properties.setProperty("password", request.password() == null ? "" : request.password());
        try {
            return DriverManager.getConnection(url, properties);
        } catch (SQLException ex) {
            throw new TargetDbException("Unable to connect to the target Postgres database: " + ex.getMessage(), ex);
        }
    }
}
