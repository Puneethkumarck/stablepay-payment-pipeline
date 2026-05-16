import nextConfig from 'eslint-config-next/core-web-vitals';

const eslintConfig = [
  ...nextConfig,
  {
    rules: {
      'react-hooks/preserve-manual-memoization': 'off',
      'react-hooks/set-state-in-effect': 'off',
    },
  },
  {
    files: ['src/components/**/*.{ts,tsx}', 'src/hooks/**/*.{ts,tsx}', 'src/app/**/*.{ts,tsx}'],
    ignores: [
      'src/app/api/**',
      'src/app/**/actions.ts',
      'src/app/**/layout.tsx',
      'src/app/**/*.test.{ts,tsx}',
    ],
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
