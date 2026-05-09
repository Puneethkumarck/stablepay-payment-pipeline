export function truncateId(id: string, prefix = 8): string {
  if (id.length <= prefix) return id;
  return `${id.slice(0, prefix)}…`;
}
