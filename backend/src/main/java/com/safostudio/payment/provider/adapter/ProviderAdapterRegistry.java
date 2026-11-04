package com.safostudio.payment.provider.adapter;

import com.safostudio.payment.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderAdapterRegistry {

  private final Map<String, ProviderAdapter> adapters;

  public ProviderAdapterRegistry(List<ProviderAdapter> adapterList) {
    this.adapters = adapterList.stream()
            .collect(Collectors.toUnmodifiableMap(ProviderAdapter::adapterKey, Function.identity()));
  }

  public ProviderAdapter get(String adapterKey) {
    ProviderAdapter adapter = adapters.get(adapterKey);
    if (adapter == null) {
      throw BusinessException.validationError("Unknown provider adapter: " + adapterKey);
    }
    return adapter;
  }

  public boolean contains(String adapterKey) {
    return adapters.containsKey(adapterKey);
  }
}