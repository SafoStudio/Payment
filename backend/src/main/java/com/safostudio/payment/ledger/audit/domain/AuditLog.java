package com.safostudio.payment.ledger.audit.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_log")
public class AuditLog {

  @Id
  private UUID id;

  private String entityType;

  private UUID recordId;

  private String action;

  private UUID userId;

  private String userRole;

  private String oldData;

  private String newData;

  private String ipAddress;

  private String userAgent;

  private Instant createdAt;

  public static AuditLog create(String entityType, UUID recordId,
                                String action, UUID userId,
                                String oldData, String newData,
                                String ipAddress, String userAgent) {
    return AuditLog.builder()
            .id(null)
            .entityType(entityType)
            .recordId(recordId)
            .action(action)
            .userId(userId)
            .oldData(oldData)
            .newData(newData)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .createdAt(Instant.now())
            .build();
  }
}