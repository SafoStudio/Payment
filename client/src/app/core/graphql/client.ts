import { createClient, cacheExchange, fetchExchange } from '@urql/core';

export const gqlClient = createClient({
  url: '/graphql',
  preferGetMethod: false,
  exchanges: [cacheExchange, fetchExchange],
  fetchOptions: () => ({
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
  }),
});
