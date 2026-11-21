import assert from 'node:assert/strict';
import test from 'node:test';
import { generateActivationCode, hashSecret, normalizeActivationCode, tokenFromRequest, validDeviceToken } from '../src/crypto-utils.js';

test('activation codes have 80 random bits and accept grouped input', () => {
  const codes = new Set(Array.from({ length: 100 }, generateActivationCode));
  assert.equal(codes.size, 100);
  for (const code of codes) {
    assert.equal(code.length, 16);
    assert.equal(normalizeActivationCode(`${code.slice(0, 4)}-${code.slice(4, 8)}-${code.slice(8, 12)}-${code.slice(12)}`), code);
  }
  assert.equal(normalizeActivationCode('0000-0000-0000-0000'), null);
  assert.equal(hashSecret('abc'), hashSecret('abc'));
});

test('device token must be exactly 32 bytes in base64url', () => {
  const token = Buffer.alloc(32, 7).toString('base64url');
  assert.equal(validDeviceToken(token), true);
  assert.equal(validDeviceToken('short'), false);
  assert.equal(tokenFromRequest(new Request('https://example.test', { headers: { authorization: `Bearer ${token}` } })), token);
  assert.equal(tokenFromRequest(new Request('https://example.test', { headers: { authorization: `Basic ${token}` } })), null);
});

