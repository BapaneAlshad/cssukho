import assert from 'node:assert/strict';
import test from 'node:test';
import { decryptSecret, encryptSecret, webhookSignature } from '../src/crypto-utils.js';
import { DEFAULT_INSTAGRAM_SCOPES, instagramConfig, instagramGraphEndpoint } from '../src/instagram.js';
import callback from '../api/instagram-callback.js';
import webhook from '../api/instagram-webhook.js';

test('provider token encryption round-trips with a fresh authenticated ciphertext', () => {
  const previous = process.env.INSTAGRAM_TOKEN_ENCRYPTION_KEY;
  process.env.INSTAGRAM_TOKEN_ENCRYPTION_KEY = Buffer.alloc(32, 7).toString('base64url');
  try {
    const ciphertext = encryptSecret('provider-token-123');
    assert.notEqual(ciphertext, encryptSecret('provider-token-123'));
    assert.equal(decryptSecret(ciphertext), 'provider-token-123');
  } finally {
    if (previous === undefined) delete process.env.INSTAGRAM_TOKEN_ENCRYPTION_KEY;
    else process.env.INSTAGRAM_TOKEN_ENCRYPTION_KEY = previous;
  }
});

test('instagram configuration stays disabled until all required secrets exist', () => {
  const keys = ['INSTAGRAM_APP_ID', 'INSTAGRAM_APP_SECRET', 'INSTAGRAM_REDIRECT_URI', 'INSTAGRAM_WEBHOOK_VERIFY_TOKEN', 'INSTAGRAM_TOKEN_ENCRYPTION_KEY'] as const;
  const previous = Object.fromEntries(keys.map(key => [key, process.env[key]]));
  keys.forEach(key => delete process.env[key]);
  try {
    assert.equal(instagramConfig(), null);
  } finally {
    keys.forEach(key => {
      const value = previous[key];
      if (value === undefined) delete process.env[key]; else process.env[key] = value;
    });
  }
});

test('instagram graph endpoint uses the configured version and webhook signature is deterministic', () => {
  const previous = {
    appId: process.env.INSTAGRAM_APP_ID,
    appSecret: process.env.INSTAGRAM_APP_SECRET,
    redirectUri: process.env.INSTAGRAM_REDIRECT_URI,
    verify: process.env.INSTAGRAM_WEBHOOK_VERIFY_TOKEN,
    encryption: process.env.INSTAGRAM_TOKEN_ENCRYPTION_KEY,
  };
  Object.assign(process.env, {
    INSTAGRAM_APP_ID: 'app', INSTAGRAM_APP_SECRET: 'secret',
    INSTAGRAM_REDIRECT_URI: 'https://example.test/callback', INSTAGRAM_WEBHOOK_VERIFY_TOKEN: 'verify',
    INSTAGRAM_TOKEN_ENCRYPTION_KEY: Buffer.alloc(32, 9).toString('base64url'),
  });
  try {
    const config = instagramConfig()!;
    assert.deepEqual(config.scopes, DEFAULT_INSTAGRAM_SCOPES);
    assert.equal(instagramGraphEndpoint(config, '/me'), 'https://graph.instagram.com/v27.0/me');
    assert.equal(webhookSignature('{"x":1}', 'secret'), '83830743d86527ff1b7e251328e56b070f2c790ca81199bdf5c31ff65fc919eb');
  } finally {
    for (const [key, value] of Object.entries({
      INSTAGRAM_APP_ID: previous.appId, INSTAGRAM_APP_SECRET: previous.appSecret,
      INSTAGRAM_REDIRECT_URI: previous.redirectUri, INSTAGRAM_WEBHOOK_VERIFY_TOKEN: previous.verify,
      INSTAGRAM_TOKEN_ENCRYPTION_KEY: previous.encryption,
    })) {
      if (value === undefined) delete process.env[key]; else process.env[key] = value;
    }
  }
});

test('public OAuth callbacks fail closed while provider configuration is absent', async () => {
  const callbackResponse = await callback.fetch(new Request('https://example.test/api/instagram-callback?state=x&code=y'));
  assert.equal(callbackResponse.status, 503);
  const webhookResponse = await webhook.fetch(new Request('https://example.test/api/instagram-webhook'));
  assert.equal(webhookResponse.status, 503);
});
