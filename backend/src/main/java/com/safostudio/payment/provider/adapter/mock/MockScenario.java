package com.safostudio.payment.provider.adapter.mock;

import com.safostudio.payment.provider.adapter.ProviderResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/**
 * Shared scenario parsing for mock adapters.
 * Config contract: {"behavior":"SUCCESS|DECLINE|TIMEOUT|MALFORMED"}.
 * Absent or empty config → SUCCESS.
 */
final class MockScenario {

  enum Behavior {
    SUCCESS,
    DECLINE,
    TIMEOUT,
    MALFORMED
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MockScenario() {
  }

  static Behavior parse(String config) {
    if (config == null || config.isBlank()) {
      return Behavior.SUCCESS;
    }
    try {
      JsonNode node = MAPPER.readTree(config);
      JsonNode behavior = node.get("behavior");
      if (behavior == null || behavior.isNull()) {
        return Behavior.SUCCESS;
      }
      return Behavior.valueOf(behavior.asString());
    } catch (RuntimeException ex) {
      return Behavior.MALFORMED;
    }
  }

  static ProviderResult apply(String adapterKey, String operation, Behavior behavior) {
    return switch (behavior) {
      case SUCCESS -> ProviderResult.approved(
              adapterKey + "-" + operation + "-" + UUID.randomUUID(),
              Map.of("provider", adapterKey, "operation", operation));
      case DECLINE -> ProviderResult.declined("MOCK_DECLINED",
              Map.of("provider", adapterKey, "operation", operation));
      case TIMEOUT -> ProviderResult.error("MOCK_TIMEOUT",
              Map.of("provider", adapterKey, "operation", operation));
      case MALFORMED -> ProviderResult.error("MALFORMED_CONFIG",
              Map.of("provider", adapterKey, "operation", operation));
    };
  }
}