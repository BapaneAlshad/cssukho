import assert from 'node:assert/strict';
import { randomBytes, randomUUID } from 'node:crypto';
import test from 'node:test';
import activate from '../../api/activate.js';
import bootstrap from '../../api/bootstrap.js';
import inbox from '../../api/inbox.js';
import interaction from '../../api/interaction.js';
import { db } from '../../src/db.js';
import { generateActivationCode, hashSecret } from '../../src/crypto-utils.js';

test('one-time pairing, authorization, inbox, completion and unique provider IDs', async () => {
  if (!process.env.TEST_DATABASE_URL) throw new Error('TEST_DATABASE_URL is required for integration tests');
  process.env.DATABASE_URL = process.env.TEST_DATABASE_URL;
  const sql = db();
  const existing = await sql`select id from sukho_private.admin_devices where revoked_at is null limit 1`;
  if (existing.length) throw new Error('Integration test requires no active paired device');

  const expiredCode = generateActivationCode();
  const code = generateActivationCode();
  const deviceId = randomUUID();
  const token = randomBytes(32).toString('base64url');
  const otherToken = randomBytes(32).toString('base64url');
  const interactionId = randomUUID();
  const providerKey = `integration-${randomUUID()}`;
  const messageId = randomUUID();
  const headers = { authorization: `Bearer ${token}` };
  const activateRequest = (value: string, credential = token) => new Request('https://example.test/api/activate', {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ code: value, deviceId, deviceToken: credential }),
  });

  try {
    await sql`
      insert into sukho_private.activation_codes (code_hash, expires_at)
      values (${hashSecret(expiredCode)}, now() - interval '1 minute'),
             (${hashSecret(code)}, now() + interval '15 minutes')
    `;
    assert.equal((await activate.fetch(activateRequest(expiredCode))).status, 409);
    assert.equal((await activate.fetch(activateRequest(code))).status, 200);
    assert.equal((await activate.fetch(activateRequest(code))).status, 200);
    assert.equal((await activate.fetch(activateRequest(code, otherToken))).status, 409);
    assert.equal((await bootstrap.fetch(new Request('https://example.test/api/bootstrap'))).status, 401);
    assert.equal((await bootstrap.fetch(new Request('https://example.test/api/bootstrap', { headers }))).status, 200);
    assert.equal((await bootstrap.fetch(new Request('https://example.test/api/bootstrap', { headers: { authorization: `Bearer ${otherToken}` } }))).status, 401);

    await sql`
      insert into sukho_private.interactions
      (id, platform, provider_account_id, kind, provider_key, sender, handle, initials)
      values (${interactionId}, 'instagram', 'integration-account', 'dm', ${providerKey}, 'Tes Integrasi', 'test', 'TI')
    `;
    await sql`
      insert into sukho_private.messages
      (id, interaction_id, provider_message_key, direction, body, status, occurred_at)
      values (${messageId}, ${interactionId}, 'integration-message', 'incoming', 'Pesan uji', 'sent', now())
    `;
    const list = await inbox.fetch(new Request('https://example.test/api/inbox', { headers }));
    assert.equal(list.status, 200);
    assert.ok((await list.json()).items.some((item: { id: string }) => item.id === interactionId));
    const detail = await interaction.fetch(new Request(`https://example.test/api/interaction?id=${interactionId}`, { headers }));
    assert.equal(detail.status, 200);
    assert.equal((await detail.json()).messages[0].text, 'Pesan uji');
    const completed = await interaction.fetch(new Request(`https://example.test/api/interaction?id=${interactionId}`, {
      method: 'PATCH', headers: { ...headers, 'content-type': 'application/json' }, body: JSON.stringify({ completed: true }),
    }));
    assert.equal(completed.status, 200);
    assert.equal((await completed.json()).completed, true);
    assert.equal((await sql`select completed_at is not null as completed from sukho_private.interactions where id = ${interactionId}`)[0].completed, true);

    await assert.rejects(sql`
      insert into sukho_private.interactions (platform, provider_account_id, kind, provider_key, sender)
      values ('instagram', 'integration-account', 'dm', ${providerKey}, 'Duplicate')
    `, (error: unknown) => (error as { code?: string }).code === '23505');
    await assert.rejects(sql`
      insert into sukho_private.messages (interaction_id, provider_message_key, direction, body, status, occurred_at)
      values (${interactionId}, 'integration-message', 'incoming', 'Duplicate', 'sent', now())
    `, (error: unknown) => (error as { code?: string }).code === '23505');

    await sql`update sukho_private.admin_devices set revoked_at = now() where id = ${deviceId}`;
    assert.equal((await bootstrap.fetch(new Request('https://example.test/api/bootstrap', { headers }))).status, 401);
  } finally {
    await sql`delete from sukho_private.interactions where id = ${interactionId}`;
    await sql`delete from sukho_private.admin_devices where id = ${deviceId}`;
    await sql`delete from sukho_private.activation_codes where code_hash in (${hashSecret(code)}, ${hashSecret(expiredCode)})`;
    await sql.end();
  }
});

