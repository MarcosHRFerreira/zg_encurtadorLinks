export function formatDateBrLocal(value: string | Date): string {
  const d = typeof value === 'string' ? new Date(value) : value;
  if (!(d instanceof Date) || isNaN(d.getTime())) return String(value ?? '');
  const pad = (n: number) => String(n).padStart(2, '0');
  const dd = pad(d.getDate());
  const mm = pad(d.getMonth() + 1);
  const yyyy = d.getFullYear();
  const HH = pad(d.getHours());
  const MM = pad(d.getMinutes());
  return `${dd}/${mm}/${yyyy} ${HH}:${MM}`;
}

export function formatDateBr(value: string | Date, opts?: { tzOffsetHours?: number }): string {
  const d = typeof value === 'string' ? new Date(value) : value;
  if (!(d instanceof Date) || isNaN(d.getTime())) return String(value ?? '');
  const offset = (opts?.tzOffsetHours ?? null);
  const base = offset == null ? d : new Date(d.getTime() + offset * 60 * 60 * 1000);
  const pad = (n: number) => String(n).padStart(2, '0');
  const dd = pad(base.getDate());
  const mm = pad(base.getMonth() + 1);
  const yyyy = base.getFullYear();
  const HH = pad(base.getHours());
  const MM = pad(base.getMinutes());
  return `${dd}/${mm}/${yyyy} ${HH}:${MM}`;
}