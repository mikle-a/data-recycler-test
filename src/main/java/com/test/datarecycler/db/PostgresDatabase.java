package com.test.datarecycler.db;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Simple postgres {@link Database} implementation based on raw JDBC.
 */
@Slf4j
@AllArgsConstructor
public class PostgresDatabase implements Database {

    private final DataSource dataSource;

    @Override
    @SneakyThrows
    public int deleteData(@NonNull String tableName,
                          @NonNull String fieldName,
                          @NonNull Instant olderThan,
                          int count) {
        final Connection connection = getConnection();
        try {
            final String query = String.format("DELETE FROM %s WHERE id IN (SELECT id FROM %s WHERE %s < ? LIMIT ?)",
                    tableName, tableName, fieldName);
            final PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setTimestamp(1, new Timestamp(olderThan.toEpochMilli()));
            preparedStatement.setInt(2, count);

            final int deletedCount = preparedStatement.executeUpdate();
            connection.commit();
            return deletedCount;
        } catch (Exception e) {
            log.error("Unexpected error", e);
            connection.rollback();
            return 0;
        } finally {
            //release connection to the pool
            connection.close();
        }
    }

    private Connection getConnection() throws SQLException {
        final Connection connection = dataSource.getConnection();
        if (!connection.getAutoCommit()) {
            connection.setAutoCommit(false);
        }
        return connection;
    }

}
