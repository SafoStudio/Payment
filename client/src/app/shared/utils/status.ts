export function paymentStatusClass(status: string): string {
  switch (status) {
    case 'SUCCEEDED':
      return 'status--succeeded';
    case 'PENDING':
      return 'status--pending';
    case 'PROCESSING':
      return 'status--processing';
    case 'FAILED':
      return 'status--failed';
    case 'REFUNDED':
      return 'status--refunded';
    case 'CANCELLED':
      return 'status--cancelled';
    default:
      return '';
  }
}

export function refundStatusClass(status: string): string {
  switch (status) {
    case 'COMPLETED':
      return 'status--completed';
    case 'PENDING':
      return 'status--pending';
    case 'PROCESSING':
      return 'status--processing';
    case 'FAILED':
      return 'status--failed';
    case 'REJECTED':
      return 'status--rejected';
    default:
      return '';
  }
}

export function transactionStatusClass(status: string): string {
  switch (status) {
    case 'COMPLETED':
      return 'status--completed';
    case 'PENDING':
      return 'status--pending';
    case 'FAILED':
      return 'status--failed';
    case 'CANCELLED':
      return 'status--cancelled';
    default:
      return '';
  }
}

export function userStatusClass(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'status--active';
    case 'BLOCKED':
      return 'status--blocked';
    case 'DELETED':
      return 'status--deleted';
    default:
      return '';
  }
}

export function walletStatusClass(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'status--active';
    case 'BLOCKED':
      return 'status--blocked';
    case 'CLOSED':
      return 'status--closed';
    default:
      return '';
  }
}

export function providerStatusClass(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'status--active';
    case 'DEGRADED':
      return 'status--degraded';
    case 'DISABLED':
      return 'status--disabled';
    default:
      return '';
  }
}
