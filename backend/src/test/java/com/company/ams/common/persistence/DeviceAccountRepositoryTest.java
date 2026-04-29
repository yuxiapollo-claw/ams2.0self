package com.company.ams.common.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@ExtendWith(MockitoExtension.class)
class DeviceAccountRepositoryTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void deleteRetriesWhenTombstoneNameCollidesBeforeUpdate() throws Exception {
        DeviceAccountRepository repository = new DeviceAccountRepository(jdbcTemplate);
        long deviceAccountId = 9L;
        long deviceNodeId = 100L;
        String accountName = "device_a_spare_09";

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(deviceAccountId)))
                .thenAnswer(invocation -> extractDeleteTarget(invocation.getArgument(1), deviceNodeId, accountName))
                .thenAnswer(invocation -> extractDeleteTarget(invocation.getArgument(1), deviceNodeId, accountName));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(deviceNodeId), anyString(), eq(deviceAccountId)))
                .thenAnswer(invocation -> {
                    String candidate = invocation.getArgument(3, String.class);
                    if (candidate.equals(buildDeletedAccountName(accountName, deviceAccountId, 0))) {
                        return 0;
                    }
                    if (candidate.equals(buildDeletedAccountName(accountName, deviceAccountId, 1))) {
                        return 0;
                    }
                    throw new AssertionError("Unexpected candidate " + candidate);
                })
                .thenAnswer(invocation -> {
                    String candidate = invocation.getArgument(3, String.class);
                    if (candidate.equals(buildDeletedAccountName(accountName, deviceAccountId, 0))) {
                        return 1;
                    }
                    if (candidate.equals(buildDeletedAccountName(accountName, deviceAccountId, 1))) {
                        return 0;
                    }
                    throw new AssertionError("Unexpected candidate " + candidate);
                });

        when(jdbcTemplate.update(anyString(), anyString(), anyLong()))
                .thenThrow(new DuplicateKeyException("duplicate"))
                .thenReturn(1);

        repository.delete(deviceAccountId);

        verify(jdbcTemplate, times(2)).update(anyString(), anyString(), eq(deviceAccountId));
    }

    @SuppressWarnings("unchecked")
    private Object extractDeleteTarget(ResultSetExtractor<?> extractor, long deviceNodeId, String accountName) throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("device_node_id")).thenReturn(deviceNodeId);
        when(resultSet.getString("account_name")).thenReturn(accountName);
        return ((ResultSetExtractor<Object>) extractor).extractData(resultSet);
    }

    private String buildDeletedAccountName(String accountName, long deviceAccountId, long attempt) {
        String suffix = "__deleted__" + deviceAccountId + "__" + Long.toString(attempt, 36);
        int prefixLength = Math.max(0, 100 - suffix.length());
        return accountName.substring(0, Math.min(accountName.length(), prefixLength)) + suffix;
    }
}
