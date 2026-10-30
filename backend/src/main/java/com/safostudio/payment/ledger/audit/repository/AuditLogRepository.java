package com.safostudio.payment.ledger.audit.repository;

import com.safostudio.payment.ledger.audit.domain.AuditLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends CrudRepository<AuditLog, UUID> {
}