package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__device_account_tombstone_normalization extends BaseJavaMigration {
    private static final int ACCOUNT_NAME_LIMIT = 100;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureNoLiveDuplicates(connection);
        for (DeletedAccount deletedAccount : deletedAccounts(connection)) {
            updateDeletedAccountName(
                    connection,
                    deletedAccount.id(),
                    allocateDeletedAccountName(
                            connection,
                            deletedAccount.deviceNodeId(),
                            deletedAccount.accountName(),
                            deletedAccount.id()));
        }
        createUniqueIndex(connection);
    }

    private void ensureNoLiveDuplicates(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                select device_node_id, account_name, count(*) as duplicate_count
                from device_account
                where deleted = 0
                group by device_node_id, account_name
                having count(*) > 1
                order by device_node_id, account_name
                """);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return;
            }

            throw new IllegalStateException(
                    "Cannot migrate device_account: active duplicate account names exist for the same device: "
                            + duplicateSummary(resultSet));
        }
    }

    private List<DeletedAccount> deletedAccounts(Connection connection) throws SQLException {
        List<DeletedAccount> deletedAccounts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                """
                select id, device_node_id, account_name
                from device_account
                where deleted = 1
                order by id
                """);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                deletedAccounts.add(new DeletedAccount(
                        resultSet.getLong("id"),
                        resultSet.getLong("device_node_id"),
                        resultSet.getString("account_name")));
            }
        }
        return deletedAccounts;
    }

    private String allocateDeletedAccountName(
            Connection connection,
            long deviceNodeId,
            String accountName,
            long deviceAccountId) throws SQLException {
        long attempt = 0;
        while (true) {
            String candidate = buildDeletedAccountName(accountName, deviceAccountId, attempt);
            if (!accountNameExists(connection, deviceNodeId, candidate, deviceAccountId)) {
                return candidate;
            }
            attempt++;
        }
    }

    private boolean accountNameExists(
            Connection connection,
            long deviceNodeId,
            String candidate,
            long deviceAccountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                select 1
                from device_account
                where device_node_id = ?
                  and account_name = ?
                  and id <> ?
                """)) {
            statement.setLong(1, deviceNodeId);
            statement.setString(2, candidate);
            statement.setLong(3, deviceAccountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void updateDeletedAccountName(Connection connection, long deviceAccountId, String accountName)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                update device_account
                set account_name = ?
                where id = ?
                  and deleted = 1
                """)) {
            statement.setString(1, accountName);
            statement.setLong(2, deviceAccountId);
            statement.executeUpdate();
        }
    }

    private void createUniqueIndex(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("create unique index uk_device_account_device_name on device_account (device_node_id, account_name)");
        }
    }

    private String duplicateSummary(ResultSet resultSet) throws SQLException {
        List<String> duplicates = new ArrayList<>();
        do {
            duplicates.add("deviceNodeId="
                    + resultSet.getLong("device_node_id")
                    + ", accountName="
                    + resultSet.getString("account_name")
                    + ", count="
                    + resultSet.getLong("duplicate_count"));
        } while (duplicates.size() < 5 && resultSet.next());
        return String.join("; ", duplicates);
    }

    private String buildDeletedAccountName(String accountName, long deviceAccountId, long attempt) {
        String suffix = "__deleted__" + deviceAccountId + "__" + Long.toString(attempt, 36);
        int prefixLength = Math.max(0, ACCOUNT_NAME_LIMIT - suffix.length());
        String prefix = accountName == null ? "" : accountName.substring(0, Math.min(accountName.length(), prefixLength));
        return prefix + suffix;
    }

    private record DeletedAccount(long id, long deviceNodeId, String accountName) {}
}
