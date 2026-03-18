package com.buyglimmer.backend.util;

import com.buyglimmer.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Component
public class DbCallUtils {

    private final DataSource dataSource;

    public DbCallUtils(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> List<T> callForList(String callSql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement callableStatement = connection.prepareCall(callSql)) {
            if (binder != null) {
                binder.bind(callableStatement);
            }
            List<T> results = new ArrayList<>();
            boolean hasResultSet = callableStatement.execute();
            if (!hasResultSet) {
                return results;
            }
            try (ResultSet resultSet = callableStatement.getResultSet()) {
                while (resultSet.next()) {
                    results.add(mapper.map(resultSet));
                }
            }
            return results;
        } catch (SQLException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Database procedure call failed", List.of(ex.getMessage()));
        }
    }

    public <T> T callForObject(String callSql, StatementBinder binder, RowMapper<T> mapper) {
        List<T> results = callForList(callSql, binder, mapper);
        if (results.isEmpty()) {
            throw new NoSuchElementException("No record found for procedure " + callSql);
        }
        return results.get(0);
    }

    public int callForUpdateCount(String callSql, StatementBinder binder) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement callableStatement = connection.prepareCall(callSql)) {
            if (binder != null) {
                binder.bind(callableStatement);
            }
            boolean hasResultSet = callableStatement.execute();
            if (hasResultSet) {
                try (ResultSet resultSet = callableStatement.getResultSet()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            }
            return callableStatement.getUpdateCount();
        } catch (SQLException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Database procedure call failed", List.of(ex.getMessage()));
        }
    }

    @FunctionalInterface
    public interface StatementBinder {
        void bind(CallableStatement callableStatement) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
