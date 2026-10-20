package com.safostudio.payment.shared.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.jspecify.annotations.NonNull;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Maps Postgres RLS violations to a FORBIDDEN GraphQL error instead of INTERNAL_ERROR.
 * <p>
 * Postgres RLS violations surface as BadSqlGrammarException via Spring's SQLState translator,
 * not DataIntegrityViolationException, so detection is by message of the root cause.
 * The driver is on runtime scope, so PSQLException is not on the compile classpath.
 */
@Component
public class RlsExceptionResolver extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(@NonNull Throwable ex, @NonNull DataFetchingEnvironment env) {
    Throwable current = ex;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && message.contains("row-level security")) {
        return GraphqlErrorBuilder.newError(env)
                .message("Access denied by row-level security policy")
                .errorType(ErrorType.FORBIDDEN)
                .build();
      }
      current = current.getCause();
    }
    return null;
  }
}