import { CombinedError } from '@urql/core';
import type { GraphQLError } from '@0no-co/graphql.web';

export type AppErrorType =
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'BAD_REQUEST'
  | 'NETWORK'
  | 'UNKNOWN';

export class AppError extends Error {
  readonly type: AppErrorType;
  readonly code?: string;

  constructor(type: AppErrorType, message: string, code?: string) {
    super(message);
    this.name = 'AppError';
    this.type = type;
    if (code !== undefined) {
      this.code = code;
    }
  }
}

export function toAppError(error: unknown): AppError {
  if (error instanceof CombinedError) {
    if (error.networkError) {
      return new AppError(
        'NETWORK',
        'Network error. Check your connection and try again.',
      );
    }

    const first = error.graphQLErrors[0];
    if (first) {
      return fromGraphQLError(first);
    }
  }

  if (error instanceof AppError) {
    return error;
  }

  if (error instanceof Error) {
    return new AppError('UNKNOWN', error.message);
  }

  return new AppError('UNKNOWN', 'Something went wrong. Please try again.');
}

function fromGraphQLError(error: GraphQLError): AppError {
  const classification = error.extensions?.['classification'];
  const code = error.extensions?.['code'];
  const type = mapClassification(classification);

  return new AppError(
    type,
    pickMessage(type, error.message),
    typeof code === 'string' ? code : undefined,
  );
}

function mapClassification(value: unknown): AppErrorType {
  switch (value) {
    case 'FORBIDDEN':
      return 'FORBIDDEN';
    case 'UNAUTHORIZED':
    case 'UNAUTHENTICATED':
      return 'UNAUTHENTICATED';
    case 'BAD_REQUEST':
    case 'BAD_USER_INPUT':
      return 'BAD_REQUEST';
    default:
      return 'UNKNOWN';
  }
}

function pickMessage(type: AppErrorType, backendMessage: string): string {
  switch (type) {
    case 'FORBIDDEN':
      return "You don't have permission to perform this action.";
    case 'UNAUTHENTICATED':
      return 'Your session has expired. Please sign in again.';
    case 'BAD_REQUEST':
      return backendMessage;
    case 'UNKNOWN':
    default:
      return backendMessage || 'Something went wrong. Please try again.';
  }
}
