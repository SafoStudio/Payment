import type { CodegenConfig } from '@graphql-codegen/cli';

const config: CodegenConfig = {
  schema: '../backend/src/main/resources/graphql/*.graphqls',
  documents: ['src/**/*.graphql'],
  generates: {
    './src/app/core/graphql/schema-types.ts': {
      plugins: ['typescript'],
      config: {
        enumsAsTypes: true,
      },
    },
    './src/app/core/graphql/documents.ts': {
      plugins: ['typescript-operations', 'typed-document-node'],
      config: {
        importSchemaTypesFrom: './src/app/core/graphql/schema-types.ts',
        enumsAsTypes: true,
      },
    },
  },
};

export default config;
