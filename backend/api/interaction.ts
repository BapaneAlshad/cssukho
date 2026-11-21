import { authorize, db } from '../src/db.js';
import { error, failed, json, readBody } from '../src/http.js';

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== 'GET' && request.method !== 'PATCH') return error('method_not_allowed', 'Metode tidak didukung.', 405);
    try {
      if (!await authorize(request)) return error('unauthorized', 'Perangkat perlu aktivasi ulang.', 401);
      const id = new URL(request.url).searchParams.get('id');
      if (!id || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id))
        return error('invalid_id', 'ID interaksi tidak valid.', 400);
      if (request.method === 'PATCH') {
        const body = await readBody(request);
        if (typeof body?.completed !== 'boolean') return error('invalid_input', 'Status tidak valid.', 400);
        const rows = await db()`
          update sukho_private.interactions
          set completed_at = ${body.completed ? new Date() : null}
          where id = ${id} returning id, completed_at is not null as completed
        `;
        return rows.length ? json(rows[0]) : error('not_found', 'Interaksi tidak ditemukan.', 404);
      }
      const rows = await db()`
        select id, platform, kind, sender, handle, initials,
          completed_at is not null as completed, updated_at as "updatedAt",
          post_caption as "postCaption", post_kind as "postKind", post_url as "postUrl",
          source_url as "sourceUrl", reply_restriction as "replyRestriction"
        from sukho_private.interactions where id = ${id} limit 1
      `;
      if (!rows.length) return error('not_found', 'Interaksi tidak ditemukan.', 404);
      const messages = await db()`
        select id, body as text, direction, status, error, occurred_at as "occurredAt"
        from sukho_private.messages where interaction_id = ${id}
        order by occurred_at, id
      `;
      return json({ interaction: rows[0], messages });
    } catch (cause) { return failed(cause); }
  },
};

