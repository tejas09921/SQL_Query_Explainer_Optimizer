package com.sqlexplainer.service;

import com.sqlexplainer.dto.ConnectionRequest;
import com.sqlexplainer.exception.TargetDbException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.stereotype.Service;

@Service
public class ExplainAnalyzeService {
    private final JdbcTargetConnectionFactory connectionFactory;

    public ExplainAnalyzeService(JdbcTargetConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public String explain(ConnectionRequest connectionRequest, String sql, boolean includeBuffers) {
        Connection connection = connectionFactory.open(connectionRequest);
        try {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL statement_timeout = '30s'");
                String explainSql = "EXPLAIN (ANALYZE, FORMAT JSON, TIMING, COSTS"
                        + (includeBuffers ? ", BUFFERS" : "")
                        + ") " + sql;
                try (ResultSet resultSet = statement.executeQuery(explainSql)) {
                    if (resultSet.next()) {
                        return resultSet.getString(1);
                    }
                    throw new TargetDbException("Postgres returned no EXPLAIN output.", null);
                }
            }
        } catch (SQLException ex) {
            throw new TargetDbException("Target database EXPLAIN ANALYZE failed: " + ex.getMessage(), ex);
        } finally {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
