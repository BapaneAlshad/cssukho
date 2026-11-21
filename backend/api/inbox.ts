import { authorize, db } from '../src/db.js';
import { error, failed, json } from '../src/http.js';

type Cursor = { at: string; id: string };
function parseCursor(raw: string | null): Cursor | null {
  if (!raw) return null;
  try {
    const value: unknown = JSON.parse(Buffer.from(raw, 'base64url').toString('utf8'));
    if (typeof value !== 'object' || value === null) return null;
    const item = value as Record<string, unknown>;
    if (typeof item.at !== 'string' || !Number.isFinite(Date.parse(item.at)) ||
        typeof item.id !== 'string' || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(item.id)) return null;
    return { at: item.at, id: item.id };
  } catch { return null; }
}

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== 'GET') return error('method_not_allowed', 'Metode tidak didukung.', 405);
    try {
      if (!await authorize(request)) return error('unauthorized', 'Perangkat perlu aktivasi ulang.', 401);
      const params = new URL(request.url).searchParams;
      const cursorText = params.get('cursor');
      const cursor = parseCursor(cursorText);
      if (cursorText && !cursor) return error('invalid_cursor', 'Halaman inbox tidak valid.', 400);
      const rows = await db()`
        select i.id, i.platform, i.kind, i.sender, i.handle, i.initials,
          i.completed_at is not null as completed, i.updated_at as "updatedAt",
          i.reply_restriction as "replyRestriction",
          coalesce(last_message.body, '') as preview
        from sukho_private.interactions i
        left join lateral (
          select body from sukho_private.messages m where m.interaction_id = i.id
          order by m.occurred_at desc, m.id desc limit 1
        ) last_message on true
        where (${cursor?.at ?? null}::timestamptz is null or
          (i.updated_at, i.id) < (${cursor?.at ?? null}::timestamptz, ${cursor?.id ?? null}::uuid))
        order by i.updated_at desc, i.id desc limit 51
      `;
      const page = rows.slice(0, 50);
      const last = page.at(-1);
      const nextCursor = rows.length > 50 && last
        ? Buffer.from(JSON.stringify({ at: last.updatedAt, id: last.id })).toString('base64url') : null;
      return json({ items: page, nextCursor });
    } catch (cause) { return failed(cause); }
  },
};

