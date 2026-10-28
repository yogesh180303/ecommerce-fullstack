package com.atelier.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jwt.secret=test-only-signing-key-that-is-at-least-32-bytes-long")
class PersistenceSchemaIntegrationTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void createsExpectedTablesAndForeignKeyRelationships() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();

            assertTrue(tableExists(metadata, "users"));
            assertTrue(tableExists(metadata, "categories"));
            assertTrue(tableExists(metadata, "product"));
            assertTrue(tableExists(metadata, "carts"));
            assertTrue(tableExists(metadata, "cart_items"));
            assertTrue(tableExists(metadata, "orders"));
            assertTrue(tableExists(metadata, "order_item"));

            assertForeignKey(metadata, "carts", "user_id", "users", "id");
            assertForeignKey(metadata, "product", "category_id", "categories", "id");
            assertForeignKey(metadata, "cart_items", "cart_id", "carts", "id");
            assertForeignKey(metadata, "cart_items", "product_id", "product", "id");
            assertForeignKey(metadata, "orders", "user_id", "users", "id");
            assertForeignKey(metadata, "order_item", "order_id", "orders", "id");
            assertForeignKey(metadata, "order_item", "product_id", "product", "id");
        }
    }

    @Test
    void enforcesCartUniquenessAndUsesDecimalMoneyColumns() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();

            assertTrue(hasUniqueIndex(metadata, "carts", Set.of("user_id")),
                    "A user must have at most one cart");
            assertTrue(hasUniqueIndex(metadata, "cart_items", Set.of("cart_id", "product_id")),
                    "A cart must not contain duplicate rows for a product");

            assertDecimalColumn(metadata, "product", "price");
            assertDecimalColumn(metadata, "orders", "total");
            assertDecimalColumn(metadata, "order_item", "unit_price");
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String expectedTable) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (normalize(tables.getString("TABLE_NAME")).equals(expectedTable)) return true;
            }
        }
        return false;
    }

    private void assertForeignKey(DatabaseMetaData metadata, String table, String column,
                                  String referencedTable, String referencedColumn) throws SQLException {
        try (ResultSet keys = metadata.getImportedKeys(null, null, findTableName(metadata, table))) {
            while (keys.next()) {
                if (normalize(keys.getString("FKCOLUMN_NAME")).equals(column)
                        && normalize(keys.getString("PKTABLE_NAME")).equals(referencedTable)
                        && normalize(keys.getString("PKCOLUMN_NAME")).equals(referencedColumn)) {
                    return;
                }
            }
        }
        throw new AssertionError("Missing foreign key " + table + "." + column + " -> "
                + referencedTable + "." + referencedColumn);
    }

    private boolean hasUniqueIndex(DatabaseMetaData metadata, String table, Set<String> expectedColumns)
            throws SQLException {
        Map<String, Set<String>> uniqueIndexes = new HashMap<>();
        try (ResultSet indexes = metadata.getIndexInfo(null, null, findTableName(metadata, table), true, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                String columnName = indexes.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    uniqueIndexes.computeIfAbsent(indexName, ignored -> new HashSet<>()).add(normalize(columnName));
                }
            }
        }
        return uniqueIndexes.values().stream().anyMatch(expectedColumns::equals);
    }

    private void assertDecimalColumn(DatabaseMetaData metadata, String table, String column) throws SQLException {
        String actualTable = findTableName(metadata, table);
        boolean found = false;
        try (ResultSet columns = metadata.getColumns(null, null, actualTable, "%")) {
            while (columns.next()) {
                if (normalize(columns.getString("COLUMN_NAME")).equals(column)) {
                    int type = columns.getInt("DATA_TYPE");
                    assertTrue(type == Types.DECIMAL || type == Types.NUMERIC,
                            "Money columns must use a decimal JDBC type");
                    assertEquals(2, columns.getInt("DECIMAL_DIGITS"),
                            "Money columns must use two decimal places");
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found, "Missing monetary column " + table + "." + column);
    }

    private String findTableName(DatabaseMetaData metadata, String expectedTable) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String actualTable = tables.getString("TABLE_NAME");
                if (normalize(actualTable).equals(expectedTable)) return actualTable;
            }
        }
        throw new AssertionError("Missing table " + expectedTable);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
