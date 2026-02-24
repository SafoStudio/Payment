package com.safostudio.payment.exchange.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("exchange_rates")
public class ExchangeRate {

    @Id
    private UUID id;

    private String fromCurrency;

    private String toCurrency;

    private BigDecimal rate;

    private BigDecimal inverseRate;

    private Instant validFrom;

    private Instant validTo;

    private String source;

    private Instant createdAt;

    /**
     * Creates a new exchange rate
     */
    public static ExchangeRate create(String fromCurrency, String toCurrency,
                                      BigDecimal rate, Instant validFrom,
                                      Instant validTo, String source) {

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }

        return ExchangeRate.builder()
                .id(UUID.randomUUID())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(rate)
                .inverseRate(BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_UP))
                .validFrom(validFrom)
                .validTo(validTo)
                .source(source)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Checks if rate is valid at given time
     */
    public boolean isValidAt(Instant moment) {
        return !moment.isBefore(validFrom) &&
                (validTo == null || !moment.isAfter(validTo));
    }

    /**
     * Converts amount using this rate
     */
    public BigDecimal convert(BigDecimal amount) {
        return amount.multiply(rate);
    }
}