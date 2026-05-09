import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: process.env.OPENAPI_INPUT ?? 'http://apps-api:8080/v3/api-docs',
  output: {
    path: 'src/lib/api-client/_generated',
    postProcess: ['biome:check'],
  },
  plugins: [
    {
      name: '@hey-api/client-fetch',
      runtimeConfigPath: './src/lib/api-client/hey-api-config.ts',
    },
    {
      name: '@hey-api/typescript',
      enums: 'javascript',
    },
    '@hey-api/sdk',
  ],
});
