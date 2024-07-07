package com.mercedes.pris.diagnosticorder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagnosticOrderAuditLogRepository extends JpaRepository<DiagnosticOrderAuditLogEntity,Long> {
}
