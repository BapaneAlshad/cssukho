import { authorizedDevice, db } from '../src/db.js';
import { error, failed, json } from '../src/http.js';
import { instagramConfig, INSTAGRAM_PLATFORM, newOAuthState, oauthStateHash } from '../src/instagram.js';

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== 'GET') return error('method_not_allowed', 'Metode tidak didukung.', 405);
    try {
      const device = await authorizedDevice(request);
      if (!device) return error('unauthorized', 'Perangkat perlu aktivasi ulang.', 401);
      const platform = new URL(request.url).searchParams.get('platform');
      if (platform !== INSTAGRAM_PLATFORM) return error('unsupported_platform', 'Koneksi TikTok belum tersedia pada tahap ini.', 400);
      const config = instagramConfig();
      if (!config) return error('provider_not_configured', 'Instagram belum dikonfigurasi pada backend.', 503);

      const state = newOAuthState();
      await db()`
        insert into sukho_private.oauth_states (state_hash, platform, device_id, redirect_uri, expires_at)
        values (${oauthStateHash(state)}, ${INSTAGRAM_PLATFORM}, ${device.id}, ${config.redirectUri}, now() + interval '10 minutes')
      `;
      const params = new URLSearchParams({
        client_id: config.appId,
        redirect_uri: config.redirectUri,
        response_type: 'code',
        scope: config.scopes.join(','),
        state,
      });
      return json({ authorizationUrl: `${config.authorizeUrl}?${params.toString()}`, expiresInSeconds: 600 });
    } catch (cause) { return failed(cause); }
  },
};
