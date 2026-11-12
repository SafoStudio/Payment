package com.safostudio.payment.notification.api;

import com.safostudio.payment.notification.dto.NotificationPageResponse;
import com.safostudio.payment.notification.dto.NotificationResponse;
import com.safostudio.payment.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @PreAuthorize("isAuthenticated()")
  @QueryMapping
  public NotificationPageResponse notifications(@Argument String status,
                                                @Argument String type,
                                                @Argument Integer limit,
                                                @Argument String after) {
    return notificationService.findPage(status, type, limit, after);
  }

  @PreAuthorize("isAuthenticated()")
  @QueryMapping
  public NotificationResponse notification(@Argument UUID id) {
    return notificationService.get(id);
  }
}