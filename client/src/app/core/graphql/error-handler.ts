import { CombinedError } from '@urql/core';
import type { GraphQLError } from '@0no-co/graphql.web';

export type AppErrorType =
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'BAD_REQUEST'
  | 'NETWORK'
  | 'UNKNOWN';

export interface AppError {
  type: AppErrorType;
  message: string;
  code?: string;
}

export function toAppError(error: unknown): AppError {
  if (error instanceof CombinedError) {
    if (error.networkError) {
      return {
        type: 'NETWORK',
        message: 'Network error. Check your connection and try again.',
      };
    }

    const first = error.graphQLErrors[0];
    if (first) {
      return fromGraphQLError(first);
    }
  }

  return {
    type: 'UNKNOWN',
    message: error instanceof Error ? error.message : 'Something went wrong. Please try again.',
  };
}

function fromGraphQLError(error: GraphQLError): AppError {
  const classification = error.extensions?.['classification'];
  const code = error.extensions?.['code'];
  const type = mapClassification(classification);

  const result: AppError = {
    type,
    message: pickMessage(type, error.message),
  };
  if (typeof code === 'string') {
    result.code = code;
  }
  return result;
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
