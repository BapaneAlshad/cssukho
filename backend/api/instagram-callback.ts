import { db } from '../src/db.js';
import { encryptSecret } from '../src/crypto-utils.js';
import { error, failed } from '../src/http.js';
import { htmlResult, instagramConfig, instagramGraphEndpoint, INSTAGRAM_PLATFORM, oauthStateHash } from '../src/instagram.js';

type TokenResponse = { access_token?: string; user_id?: string | number; expires_in?: number; token_type?: string };
type ProfileResponse = { id?: string; user_id?: string | number; username?: string };

async function exchangeCode(config: NonNullable<ReturnType<typeof instagramConfig>>, code: string, redirectUri: string): Promise<TokenResponse> {
  const form = new URLSearchParams({
    client_id: config.appId,
    client_secret: config.appSecret,
    grant_type: 'authorization_code',
    redirect_uri: redirectUri,
    code,
  });
  const response = await fetch(config.tokenUrl, { method: 'POST', headers: { 'content-type': 'application/x-www-form-urlencoded' }, body: form });
  const payload = await response.json().catch(() => ({})) as TokenResponse & { error_message?: string };
  if (!response.ok || typeof payload.access_token !== 'string') throw new Error(payload.error_message || 'Instagram menolak kode login.');
  return payload;
}

async function fetchProfile(config: NonNullable<ReturnType<typeof instagramConfig>>, accessToken: string): Promise<ProfileResponse> {
  const url = new URL(instagramGraphEndpoint(config, '/me'));
  url.searchParams.set('fields', 'user_id,username');
  url.searchParams.set('access_token', accessToken);
  const response = await fetch(url, { headers: { accept: 'application/json' } });
  const payload = await response.json().catch(() => ({})) as ProfileResponse & { error?: { message?: string } };
  if (!response.ok || (!payload.user_id && !payload.id)) throw new Error(payload.error?.message || 'Profil Instagram tidak dapat dibaca.');
  return payload;
}

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== 'GET') return error('method_not_allowed', 'Metode tidak didukung.', 405);
    const url = new URL(request.url);
    const state = url.searchParams.get('state');
    const code = url.searchParams.get('code');
    if (url.searchParams.get('error')) return htmlResult('Koneksi Instagram dibatalkan', 'Tidak ada perubahan pada akun SUKHO RAMEN.');
    if (!state || !code) return error('invalid_callback', 'Callback Instagram tidak lengkap.', 400);
    const config = instagramConfig();
    if (!config) return error('provider_not_configured', 'Instagram belum dikonfigurasi pada backend.', 503);

    try {
      const stateHash = oauthStateHash(state);
      const claimed = await db().begin(async tx => {
        const rows = await tx`
          select state_hash, platform, device_id, redirect_uri, expires_at, used_at
          from sukho_private.oauth_states where state_hash = ${stateHash} for update
        `;
        if (!rows.length || rows[0].used_at || new Date(rows[0].expires_at as string).getTime() <= Date.now()) return null;
        await tx`update sukho_private.oauth_states set used_at = now() where state_hash = ${stateHash}`;
        const deviceRows = await tx`
          select id from sukho_private.admin_devices where id = ${rows[0].device_id} and revoked_at is null limit 1
        `;
        if (!deviceRows.length) return null;
        return { deviceId: rows[0].device_id as string, redirectUri: rows[0].redirect_uri as string };
      });
      if (!claimed) return error('invalid_state', 'Sesi koneksi sudah kedaluwarsa atau sudah dipakai.', 400);

      const token = await exchangeCode(config, code, claimed.redirectUri);
      const profile = await fetchProfile(config, token.access_token!);
      const providerAccountId = String(profile.user_id ?? profile.id);
      const scopes = config.scopes;
      const tokenExpiresAt = typeof token.expires_in === 'number' ? new Date(Date.now() + token.expires_in * 1000) : null;
      await db().begin(async tx => {
        await tx`
          insert into sukho_private.platform_tokens (platform, provider_account_id, handle, access_token_ciphertext, token_expires_at, scopes, updated_at)
          values (${INSTAGRAM_PLATFORM}, ${providerAccountId}, ${profile.username ?? null}, ${encryptSecret(token.access_token!)}, ${tokenExpiresAt}, ${scopes}, now())
          on conflict (platform) do update set provider_account_id = excluded.provider_account_id,
            handle = excluded.handle, access_token_ciphertext = excluded.access_token_ciphertext,
            token_expires_at = excluded.token_expires_at, scopes = excluded.scopes, updated_at = now()
        `;
        await tx`
          update sukho_private.platform_connections
          set status = 'connected', handle = ${profile.username ?? null}, can_read = true, can_reply = true, updated_at = now()
          where platform = ${INSTAGRAM_PLATFORM}
        `;
      });
      return htmlResult('Instagram terhubung', 'Akun Instagram berhasil terhubung dengan SUKHO RAMEN.');
    } catch (cause) {
      return failed(cause);
    }
  },
};
