/**
 * Formats an ISO-8601 instant from GraphQL using the runtime locale and the
 * browser time zone. Shape is fixed (y/m/d + h:m) so list and detail pages
 * render timestamps consistently.
 */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}
