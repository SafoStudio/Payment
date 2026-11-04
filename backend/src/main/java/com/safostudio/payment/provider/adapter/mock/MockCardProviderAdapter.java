package com.safostudio.payment.provider.adapter.mock;

import com.safostudio.payment.provider.adapter.ProviderAdapter;
import com.safostudio.payment.provider.adapter.ProviderRequest;
import com.safostudio.payment.provider.adapter.ProviderResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MockCardProviderAdapter implements ProviderAdapter {

  @Override
  public String adapterKey() {
    return "mock-card";
  }

  @Override
  public ProviderResult authorize(ProviderRequest request) {
    if (!"CARD".equals(request.method())) {
      return ProviderResult.error("UNSUPPORTED_METHOD", Map.of("method", request.method()));
    }
    MockScenario.Behavior behavior = MockScenario.parse(request.config());
    return MockScenario.apply(adapterKey(), "authorize", behavior);
  }

  @Override
  public ProviderResult refund(ProviderRequest request) {
    MockScenario.Behavior behavior = MockScenario.parse(request.config());
    return MockScenario.apply(adapterKey(), "refund", behavior);
  }
}