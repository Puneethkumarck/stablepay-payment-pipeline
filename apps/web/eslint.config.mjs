import nextConfig from 'eslint-config-next/core-web-vitals';

const eslintConfig = [
  ...nextConfig,
  {
    files: ['src/components/**/*.{ts,tsx}', 'src/hooks/**/*.{ts,tsx}', 'src/app/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['~/server/**'],
              message: 'Server modules cannot be imported from client components.',
            },
          ],
        },
      ],
    },
  },
  {
    ignores: ['.next/**', 'src/lib/api-client/_generated/**'],
  },
];

export default eslintConfig;
