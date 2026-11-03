/**
 * Formats a GraphQL money string (String!, backend BigDecimal) using the
 * runtime locale and the given ISO-4217 currency code. Non-numeric input
 * is returned unchanged so callers see the raw backend value instead of NaN.
 */
export function formatAmount(amount: string, currency: string): string {
  const value = Number(amount);
  if (!Number.isFinite(value)) return amount;
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency,
  }).format(value);
}
