package com.safostudio.payment.ledger.audit.dto;

import com.safostudio.payment.ledger.audit.domain.AuditLog;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AuditLogResponse {
  UUID id;
  String entityType;
  UUID recordId;
  String action;
  UUID userId;
  String userRole;
  String oldData;
  String newData;
  String ipAddress;
  String userAgent;
  Instant createdAt;

  public static AuditLogResponse fromDomain(AuditLog log) {
    return AuditLogResponse.builder()
            .id(log.getId())
            .entityType(log.getEntityType())
            .recordId(log.getRecordId())
            .action(log.getAction())
            .userId(log.getUserId())
            .userRole(log.getUserRole())
            .oldData(log.getOldData())
            .newData(log.getNewData())
            .ipAddress(log.getIpAddress())
            .userAgent(log.getUserAgent())
            .createdAt(log.getCreatedAt())
            .build();
  }
}