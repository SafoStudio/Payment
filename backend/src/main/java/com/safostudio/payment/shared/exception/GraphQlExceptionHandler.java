package com.safostudio.payment.shared.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    if (ex instanceof BusinessException be) {
      return GraphqlErrorBuilder.newError(env)
              .message(be.getMessage())
              .errorType(ErrorType.BAD_REQUEST)
              .extensions(Map.of("code", be.getCode().name()))
              .build();
    }
    return null;
  }
}