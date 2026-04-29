package com.company.ams.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

@JdbcTest(properties = {
        "spring.flyway.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:flyway-schema;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class FlywaySchemaTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void createsCoreTables() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(tableNames(metaData)).contains(
                    "audit_log",
                    "asset_node",
                    "device_account",
                    "device_account_role",
                    "request_order",
                    "sys_department",
                    "sys_user");
        }
    }

    @Test
    void createsCriticalColumnsConstraintsAndIndexes() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            assertThat(hasColumn(metaData, "device_account_role", "source_request_id")).isTrue();
            assertThat(hasColumn(metaData, "request_order", "request_no")).isTrue();

            assertThat(hasUniqueIndex(metaData, "sys_user", "user_code")).isTrue();
            assertThat(hasUniqueIndex(metaData, "device_account", "device_node_id", "account_name")).isTrue();
            assertThat(hasUniqueIndex(metaData, "device_account_role", "device_account_id", "role_node_id")).isTrue();

            assertThat(hasIndex(metaData, "sys_user", "department_id")).isTrue();
            assertThat(hasIndex(metaData, "asset_node", "parent_id")).isTrue();
            assertThat(hasIndex(metaData, "request_order", "target_device_node_id")).isTrue();

            assertThat(hasForeignKey(metaData, "sys_user", "department_id", "sys_department")).isTrue();
            assertThat(hasForeignKey(metaData, "device_account", "user_id", "sys_user")).isTrue();
            assertThat(hasForeignKey(metaData, "device_account_role", "role_node_id", "asset_node")).isTrue();
            assertThat(isNullable(metaData, "device_account", "user_id")).isTrue();
        }
    }

    @Test
    void migrationTombstonesDeletedAccountsSoLiveUniqueKeysRemainAvailable() throws Exception {
        String url = "jdbc:h2:mem:flyway-schema-tombstone-" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway preV6 = flyway(url, "5");
        preV6.migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(manualDataSource(url));
        jdbcTemplate.update(
                """
                insert into device_account (
                  id,
                  user_id,
                  device_node_id,
                  account_name,
                  account_status,
                  source_type,
                  remark,
                  deleted
                ) values (123, ?, ?, ?, 'ENABLED', 'MANUAL', 'legacy tombstone', 1)
                """,
                null,
                100L,
                "foo");
        for (int attempt = 0; attempt <= 1500; attempt++) {
            jdbcTemplate.update(
                    """
                    insert into device_account (
                      user_id,
                      device_node_id,
                      account_name,
                      account_status,
                      source_type,
                      remark,
                      deleted
                    ) values (?, ?, ?, 'ENABLED', 'MANUAL', 'conflicting live name', 0)
                    """,
                    null,
                    100L,
                    buildDeletedAccountName("foo", 123L, attempt));
        }

        flyway(url, null).migrate();

        String migratedDeletedName = jdbcTemplate.queryForObject(
                """
                select account_name
                from device_account
                where remark = 'legacy tombstone'
                """,
                String.class);
        assertThat(migratedDeletedName)
                .isEqualTo(buildDeletedAccountName("foo", 123L, 1501));

        jdbcTemplate.update(
                """
                insert into device_account (
                  user_id,
                  device_node_id,
                  account_name,
                  account_status,
                  source_type,
                  remark,
                  deleted
                ) values (?, ?, ?, 'ENABLED', 'MANUAL', 'live replacement', 0)
                """,
                null,
                100L,
                "foo");

        Integer liveCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from device_account
                where device_node_id = 100
                  and account_name = 'foo'
                  and deleted = 0
                """,
                Integer.class);
        assertThat(liveCount).isEqualTo(1);
    }

    @Test
    void migrationFailsFastWhenLiveDeviceAccountsShareSameDeviceAndAccountName() throws Exception {
        String url = "jdbc:h2:mem:flyway-schema-live-duplicates-" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        flyway(url, "5").migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(manualDataSource(url));
        jdbcTemplate.update(
                """
                insert into device_account (
                  user_id,
                  device_node_id,
                  account_name,
                  account_status,
                  source_type,
                  remark,
                  deleted
                ) values (?, ?, ?, 'ENABLED', 'MANUAL', 'duplicate live row 1', 0)
                """,
                null,
                100L,
                "legacy_live_duplicate");
        jdbcTemplate.update(
                """
                insert into device_account (
                  user_id,
                  device_node_id,
                  account_name,
                  account_status,
                  source_type,
                  remark,
                  deleted
                ) values (?, ?, ?, 'ENABLED', 'MANUAL', 'duplicate live row 2', 0)
                """,
                null,
                100L,
                "legacy_live_duplicate");

        assertThatThrownBy(() -> flyway(url, null).migrate())
                .hasRootCauseMessage(
                        "Cannot migrate device_account: active duplicate account names exist for the same device: "
                                + "deviceNodeId=100, accountName=legacy_live_duplicate, count=2");
    }

    private String buildDeletedAccountName(String accountName, long deviceAccountId, long attempt) {
        String suffix = "__deleted__" + deviceAccountId + "__" + Long.toString(attempt, 36);
        int prefixLength = Math.max(0, 100 - suffix.length());
        return accountName.substring(0, Math.min(accountName.length(), prefixLength)) + suffix;
    }

    private List<String> tableNames(DatabaseMetaData metaData) throws Exception {
        List<String> tableNames = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                tableNames.add(normalize(rs.getString("TABLE_NAME")));
            }
        }
        return tableNames;
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private boolean isNullable(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return rs.next() && rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
        }
    }

    private boolean hasUniqueIndex(DatabaseMetaData metaData, String tableName, String... columns) throws Exception {
        return hasIndex(metaData, tableName, true, columns);
    }

    private boolean hasIndex(DatabaseMetaData metaData, String tableName, String... columns) throws Exception {
        return hasIndex(metaData, tableName, false, columns);
    }

    private boolean hasIndex(DatabaseMetaData metaData, String tableName, boolean unique, String... columns) throws Exception {
        List<String> expectedColumns = normalize(columns);
        List<IndexMetadata> indexes = new ArrayList<>();

        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName, unique, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName == null || columnName == null) {
                    continue;
                }

                short type = rs.getShort("TYPE");
                if (type == DatabaseMetaData.tableIndexStatistic) {
                    continue;
                }

                indexes.add(new IndexMetadata(
                        normalize(indexName),
                        normalize(columnName),
                        !rs.getBoolean("NON_UNIQUE"),
                        rs.getShort("ORDINAL_POSITION")));
            }
        }

        return indexes.stream()
                .filter(index -> index.unique == unique)
                .map(index -> index.name)
                .distinct()
                .anyMatch(indexName -> indexes.stream()
                        .filter(index -> index.name.equals(indexName))
                        .sorted((left, right) -> Short.compare(left.position, right.position))
                        .map(index -> index.column)
                        .toList()
                        .equals(expectedColumns));
    }

    private boolean hasForeignKey(DatabaseMetaData metaData, String tableName, String columnName, String referencedTable)
            throws Exception {
        try (ResultSet rs = metaData.getImportedKeys(null, null, tableName)) {
            while (rs.next()) {
                if (normalize(columnName).equals(normalize(rs.getString("FKCOLUMN_NAME")))
                        && normalize(referencedTable).equals(normalize(rs.getString("PKTABLE_NAME")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> normalize(String... values) {
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            normalized.add(normalize(value));
        }
        return normalized;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private Flyway flyway(String url, String targetVersion) {
        Flyway.configure()
                .cleanDisabled(true);
        var configuration = Flyway.configure()
                .dataSource(manualDataSource(url))
                .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        return configuration.load();
    }

    private DataSource manualDataSource(String url) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Driver> driverClass = (Class<? extends Driver>) Class.forName("org.h2.Driver");
            Driver driver = driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(driver);
            return new SimpleDriverDataSource(driver, url, "sa", "");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create manual datasource", ex);
        }
    }

    private record IndexMetadata(String name, String column, boolean unique, short position) {}
}
