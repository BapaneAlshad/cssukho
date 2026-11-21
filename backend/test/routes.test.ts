import assert from 'node:assert/strict';
import test from 'node:test';
import activate from '../api/activate.js';
import bootstrap from '../api/bootstrap.js';
import inbox from '../api/inbox.js';
import interaction from '../api/interaction.js';

test('protected routes reject requests without a device credential', async () => {
  for (const [handler, url] of [
    [bootstrap, 'https://example.test/api/bootstrap'],
    [inbox, 'https://example.test/api/inbox'],
    [interaction, 'https://example.test/api/interaction?id=00000000-0000-4000-8000-000000000001'],
  ] as const) {
    const response = await handler.fetch(new Request(url));
    assert.equal(response.status, 401);
    assert.equal(response.headers.get('cache-control'), 'no-store');
  }
});

test('activation rejects malformed input before database access', async () => {
  const response = await activate.fetch(new Request('https://example.test/api/activate', {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ code: 'BAD', deviceToken: 'short', deviceId: 'wrong' }),
  }));
  assert.equal(response.status, 400);
});

