package com.sqlexplainer.util;

import com.sqlexplainer.exception.InvalidSqlException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SqlSafetyValidator {
    private static final Pattern MULTIPLE_STATEMENTS = Pattern.compile(";\\s*\\S", Pattern.DOTALL);
    private static final Set<String> DDL_ADMIN_KEYWORDS = Set.of(
            "DROP", "TRUNCATE", "ALTER", "GRANT", "REVOKE", "VACUUM");
    private static final Pattern CREATE_EXTENSION = Pattern.compile("\\bCREATE\\s+EXTENSION\\b", Pattern.CASE_INSENSITIVE);

    public void validate(String sql, boolean allowNonSelect) {
        if (sql == null || sql.isBlank()) {
            throw new InvalidSqlException("SQL must not be empty.");
        }
        String trimmed = sql.trim();
        String scrubbedRaw = stripQuotedTextAndComments(trimmed);
        if (MULTIPLE_STATEMENTS.matcher(scrubbedRaw).find()) {
            throw new InvalidSqlException("Multiple SQL statements are not allowed.");
        }
        String scrubbed = scrubbedRaw.toUpperCase(Locale.ROOT);
        for (String keyword : DDL_ADMIN_KEYWORDS) {
            if (Pattern.compile("\\b" + keyword + "\\b").matcher(scrubbed).find()) {
                throw new InvalidSqlException(keyword + " is not allowed. EXPLAIN ANALYZE executes the query even though this service rolls back.");
            }
        }
        if (CREATE_EXTENSION.matcher(scrubbed).find()) {
            throw new InvalidSqlException("CREATE EXTENSION is not allowed. EXPLAIN ANALYZE executes the query even though this service rolls back.");
        }
        if (!allowNonSelect && !startsWithSelectOrWith(scrubbed)) {
            throw new InvalidSqlException("Only SELECT or WITH statements are allowed unless allowNonSelect is true. Non-SELECT statements can fire triggers and side effects even though the transaction is rolled back.");
        }
    }

    private boolean startsWithSelectOrWith(String sql) {
        return sql.startsWith("SELECT ") || sql.equals("SELECT")
                || sql.startsWith("WITH ") || sql.equals("WITH");
    }

    private String stripQuotedTextAndComments(String sql) {
        String withoutLineComments = sql.replaceAll("(?m)--.*$", " ");
        String withoutBlockComments = withoutLineComments.replaceAll("(?s)/\\*.*?\\*/", " ");
        String withoutSingleQuoted = withoutBlockComments.replaceAll("'([^']|'')*'", "''");
        return withoutSingleQuoted.replaceAll("\"([^\"]|\"\")*\"", "\"\"");
    }
}
