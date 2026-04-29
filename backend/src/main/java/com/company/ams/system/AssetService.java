package com.company.ams.system;

import com.company.ams.common.persistence.AssetRepository;
import com.company.ams.common.persistence.AssetRepository.AssetNodeRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AssetService {
    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<AssetNode> tree() {
        List<AssetNodeRecord> rows = assetRepository.findAllEnabled();
        Map<Long, List<AssetNodeRecord>> childrenByParent = new HashMap<>();
        for (AssetNodeRecord row : rows) {
            childrenByParent.computeIfAbsent(row.parentId(), ignored -> new ArrayList<>()).add(row);
        }
        childrenByParent.values().forEach(children -> children.sort(Comparator
                .comparingInt(AssetNodeRecord::sortNo)
                .thenComparingLong(AssetNodeRecord::id)));
        return buildChildren(childrenByParent, null);
    }

    private List<AssetNode> buildChildren(Map<Long, List<AssetNodeRecord>> childrenByParent, Long parentId) {
        return childrenByParent.getOrDefault(parentId, List.of()).stream()
                .map(row -> new AssetNode(
                        row.id(),
                        row.nodeName(),
                        row.nodeType(),
                        buildChildren(childrenByParent, row.id())))
                .toList();
    }
}

record AssetNode(
        Long id,
        String nodeName,
        String nodeType,
        List<AssetNode> children) {}
