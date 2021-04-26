package com.test.datarecycler.db;

import java.time.Instant;

/**
 * Represents access point to the database
 */
public interface Database {

    /**
     * Delete data older than specified datetime
     * @param tableName destination table
     * @param fieldName datetime field name to query old row
     * @param olderThan how old data should be deleted
     * @param count count of rows to be deleted
     * @return actually deleted rows count. Zero means there is not more records to delete.
     */
    int deleteData(String tableName, String fieldName, Instant olderThan, int count);
}
