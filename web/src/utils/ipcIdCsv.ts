export function parseIpcIdCsv(content: string) {
  const ids: string[] = [];
  const seen = new Set<string>();

  for (const value of content.split(',')) {
    const id = value.trim();
    if (!id || seen.has(id)) {
      continue;
    }

    seen.add(id);
    ids.push(id);
  }

  return ids;
}
