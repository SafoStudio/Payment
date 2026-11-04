package com.safostudio.payment.provider.adapter;

public interface ProviderAdapter {

  String adapterKey();

  ProviderResult authorize(ProviderRequest request);

  ProviderResult refund(ProviderRequest request);
}