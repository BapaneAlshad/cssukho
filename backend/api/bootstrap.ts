import { authorize, db } from '../src/db.js';
import { error, failed, json } from '../src/http.js';

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== 'GET') return error('method_not_allowed', 'Metode tidak didukung.', 405);
    try {
      if (!await authorize(request)) return error('unauthorized', 'Perangkat perlu aktivasi ulang.', 401);
      const rows = await db()`
        select platform, status, handle, can_read as "canRead", can_reply as "canReply"
        from sukho_private.platform_connections order by platform
      `;
      return json({ accounts: rows });
    } catch (cause) { return failed(cause); }
  },
};

