import { db } from '../src/db.js';
import { constantTimeEqual, webhookSignature } from '../src/crypto-utils.js';
import { error, failed, json } from '../src/http.js';
import { instagramConfig } from '../src/instagram.js';

function eventKeys(payload: unknown, raw: string): string[] {
  if (!payload || typeof payload !== 'object') return [webhookSignature(raw, 'sukho-event-key')];
  const root = payload as Record<string, unknown>;
  const entries = Array.isArray(root.entry) ? root.entry : [];
  if (!entries.length) return [webhookSignature(raw, 'sukho-event-key')];
  return entries.map((entry, index) => {
    const item = entry && typeof entry === 'object' ? entry as Record<string, unknown> : {};
    const source = item.id ?? item.time ?? `${raw}:${index}`;
    const changes = JSON.stringify(item.changes ?? item.messaging ?? item);
    return webhookSignature(`${String(source)}:${changes}`, 'sukho-event-key');
  });
}

export default {
  async fetch(request: Request): Promise<Response> {
    const config = instagramConfig();
    if (!config) return error('provider_not_configured', 'Instagram belum dikonfigurasi pada backend.', 503);
    if (request.method === 'GET') {
      const params = new URL(request.url).searchParams;
      if (params.get('hub.mode') !== 'subscribe' || params.get('hub.verify_token') !== config.webhookVerifyToken)
        return error('invalid_webhook_verification', 'Verifikasi webhook ditolak.', 403);
      return new Response(params.get('hub.challenge') ?? '', { status: 200, headers: { 'content-type': 'text/plain; charset=utf-8', 'cache-control': 'no-store' } });
    }
    if (request.method !== 'POST') return error('method_not_allowed', 'Metode tidak didukung.', 405);
    try {
      const raw = await request.text();
      const header = request.headers.get('x-hub-signature-256') ?? '';
      const expected = `sha256=${webhookSignature(raw, config.appSecret)}`;
      if (!constantTimeEqual(header, expected)) return error('invalid_signature', 'Signature webhook tidak valid.', 403);
      const payload: unknown = JSON.parse(raw);
      const keys = eventKeys(payload, raw);
      let inserted = 0;
      for (const key of keys) {
        const rows = await db()`
          insert into sukho_private.provider_events (platform, provider_event_key, payload)
          values ('instagram', ${key}, ${raw}::jsonb)
          on conflict (platform, provider_event_key) do nothing
          returning provider_event_key
        `;
        inserted += rows.length;
      }
      return json({ received: true, inserted });
    } catch (cause) { return failed(cause); }
  },
};
