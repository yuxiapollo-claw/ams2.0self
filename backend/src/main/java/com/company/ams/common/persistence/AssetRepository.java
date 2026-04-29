package com.company.ams.common.persistence;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssetRepository {
    private final JdbcTemplate jdbcTemplate;

    public AssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AssetNodeRecord> findAllEnabled() {
        return jdbcTemplate.query(
                """
                select id, node_name, node_type, parent_id, sort_no
                from asset_node
                where deleted = 0 and status = 'ENABLED'
                order by level_num, sort_no, id
                """,
                (rs, rowNum) -> new AssetNodeRecord(
                        rs.getLong("id"),
                        rs.getString("node_name"),
                        rs.getString("node_type"),
                        (Long) rs.getObject("parent_id"),
                        rs.getInt("sort_no")));
    }

    public record AssetNodeRecord(
            long id,
            String nodeName,
            String nodeType,
            Long parentId,
            int sortNo) {}
}
