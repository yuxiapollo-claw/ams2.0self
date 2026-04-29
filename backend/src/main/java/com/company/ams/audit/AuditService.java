package com.company.ams.audit;

import com.company.ams.common.persistence.AuditRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public AuditLogListPayload logs() {
        List<AuditLogRow> rows = auditRepository.findAll();
        return new AuditLogListPayload(rows, rows.size());
    }
}

record AuditLogListPayload(List<AuditLogRow> list, int total) {}
