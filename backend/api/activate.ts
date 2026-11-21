import { db } from '../src/db.js';
import { error, failed, json, readBody } from '../src/http.js';
import { hashSecret, normalizeActivationCode, validDeviceToken } from '../src/crypto-utils.js';

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== 'POST') return error('method_not_allowed', 'Metode tidak didukung.', 405);
    const body = await readBody(request);
    const code = normalizeActivationCode(body?.code);
    const token = body?.deviceToken;
    const deviceId = body?.deviceId;
    if (!code || !validDeviceToken(token) || typeof deviceId !== 'string' ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(deviceId)) {
      return error('invalid_input', 'Kode atau data perangkat tidak valid.', 400);
    }
    const codeHash = hashSecret(code);
    const tokenHash = hashSecret(token);
    try {
      const result = await db().begin(async tx => {
        await tx`select pg_advisory_xact_lock(85127410)`;
        const rows = await tx`
          select code_hash, expires_at > now() as valid, used_at from sukho_private.activation_codes
          where code_hash = ${codeHash} for update
        `;
        if (!rows.length) return 'invalid';
        if (rows[0].used_at) {
          const active = await tx`
            select id from sukho_private.admin_devices
            where activation_code_hash = ${codeHash} and token_hash = ${tokenHash}
              and id = ${deviceId} and revoked_at is null limit 1
          `;
          return active.length ? 'active' : 'used';
        }
        if (!rows[0].valid) return 'expired';
        await tx`update sukho_private.admin_devices set revoked_at = now() where revoked_at is null`;
        await tx`
          insert into sukho_private.admin_devices (id, token_hash, activation_code_hash)
          values (${deviceId}, ${tokenHash}, ${codeHash})
        `;
        await tx`update sukho_private.activation_codes set used_at = now() where code_hash = ${codeHash}`;
        return 'active';
      });
      if (result !== 'active') return error('activation_' + result, 'Kode aktivasi tidak berlaku. Minta kode baru.', 409);
      return json({ active: true });
    } catch (cause) { return failed(cause); }
  },
};

