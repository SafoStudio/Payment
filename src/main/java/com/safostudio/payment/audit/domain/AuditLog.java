package com.safostudio.payment.audit.domain;

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
@Table("audit_logs")
public class AuditLog {

    @Id
    private UUID id;

    private String entityType;

    private UUID entityId;

    private String action;

    private UUID userId;

    private String userRole;

    private String oldValue;

    private String newValue;

    private String ipAddress;

    private String userAgent;

    private Instant createdAt;

    /**
     * Creates an audit log entry
     */
    public static AuditLog create(String entityType, UUID entityId,
                                  String action, UUID userId,
                                  String oldValue, String newValue,
                                  String ipAddress, String userAgent) {
        return AuditLog.builder()
                .id(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .userId(userId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(Instant.now())
                .build();
    }
}