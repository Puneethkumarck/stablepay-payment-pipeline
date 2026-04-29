module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'scope-enum': [2, 'always', [
      'schemas', 'api', 'web', 'simulator', 'llm-agent',
      'agent-tools-mcp', 'dlq-tools', 'lakehouse-jobs',
      'flink-jobs', 'infra', 'deploy', 'ci', 'docs', 'tooling', 'auth'
    ]],
  },
};
