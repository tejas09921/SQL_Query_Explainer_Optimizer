package com.sqlexplainer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqlexplainer.exception.InvalidSqlException;
import com.sqlexplainer.util.SqlSafetyValidator;
import org.junit.jupiter.api.Test;

class SqlSafetyValidatorTests {
    private final SqlSafetyValidator validator = new SqlSafetyValidator();

    @Test
    void rejectsMultipleStatements() {
        assertThatThrownBy(() -> validator.validate("select 1; select 2", false))
                .isInstanceOf(InvalidSqlException.class);
    }

    @Test
    void rejectsAdminKeywordsEvenWhenNonSelectAllowed() {
        assertThatThrownBy(() -> validator.validate("vacuum orders", true))
                .isInstanceOf(InvalidSqlException.class);
    }

    @Test
    void rejectsNonSelectByDefault() {
        assertThatThrownBy(() -> validator.validate("update orders set status = 'paid'", false))
                .isInstanceOf(InvalidSqlException.class);
    }
}
