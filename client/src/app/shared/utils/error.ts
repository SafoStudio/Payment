export function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'object' && error !== null && 'message' in error) {
    const raw = (error as { message: unknown }).message;
    if (typeof raw === 'string') {
      return raw;
    }
  }
  return fallback;
}
