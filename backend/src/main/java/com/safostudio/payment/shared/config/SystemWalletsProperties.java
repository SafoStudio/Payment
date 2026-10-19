package com.safostudio.payment.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.system")
public class SystemWalletsProperties {

  private UUID topupWalletId;
  private UUID feeWalletId;
}