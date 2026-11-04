package com.safostudio.payment.ledger.audit.api;

import com.safostudio.payment.ledger.audit.dto.AuditLogPageResponse;
import com.safostudio.payment.ledger.audit.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AuditLogController {

  private final AuditLogQueryService auditLogQueryService;

  @PreAuthorize("hasAuthority('audit:read:all')")
  @QueryMapping
  public AuditLogPageResponse auditLogs(
          @Argument String entityType,
          @Argument String action,
          @Argument UUID userId,
          @Argument Integer limit,
          @Argument String after
  ) {
    return auditLogQueryService.findPage(entityType, action, userId, limit, after);
  }
}
