import { createCipheriv, createDecipheriv, createHash, createHmac, randomBytes, timingSafeEqual } from 'node:crypto';

export const ACTIVATION_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

export function hashSecret(value: string): string {
  return createHash('sha256').update(value, 'utf8').digest('hex');
}

export function generateActivationCode(): string {
  const bytes = randomBytes(10);
  let bits = 0;
  let buffer = 0;
  let result = '';
  for (const byte of bytes) {
    buffer = (buffer << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      result += ACTIVATION_ALPHABET[(buffer >>> bits) & 31];
      buffer &= (1 << bits) - 1;
    }
  }
  return result;
}

export function normalizeActivationCode(input: unknown): string | null {
  if (typeof input !== 'string') return null;
  const value = input.toUpperCase().replace(/[\s-]/g, '');
  return value.length === 16 && [...value].every(char => ACTIVATION_ALPHABET.includes(char)) ? value : null;
}

export function validDeviceToken(input: unknown): input is string {
  return typeof input === 'string' && /^[A-Za-z0-9_-]{43}$/.test(input);
}

export function tokenFromRequest(request: Request): string | null {
  const value = request.headers.get('authorization');
  const match = value?.match(/^Bearer ([A-Za-z0-9_-]{43})$/);
  return match?.[1] ?? null;
}

export function constantTimeEqual(a: string, b: string): boolean {
  const left = Buffer.from(a);
  const right = Buffer.from(b);
  return left.length === right.length && timingSafeEqual(left, right);
}

function encryptionKey(): Buffer {
  const encoded = process.env.INSTAGRAM_TOKEN_ENCRYPTION_KEY;
  if (!encoded) throw new Error('INSTAGRAM_TOKEN_ENCRYPTION_KEY is required');
  const key = Buffer.from(encoded, 'base64url');
  if (key.length !== 32) throw new Error('INSTAGRAM_TOKEN_ENCRYPTION_KEY must decode to 32 bytes');
  return key;
}

/** Encrypt a provider token before it is persisted. The key never leaves Vercel. */
export function encryptSecret(value: string): string {
  const iv = randomBytes(12);
  const cipher = createCipheriv('aes-256-gcm', encryptionKey(), iv);
  const ciphertext = Buffer.concat([cipher.update(value, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return [iv, ciphertext, tag].map(part => part.toString('base64url')).join('.');
}

export function decryptSecret(value: string): string {
  const parts = value.split('.');
  if (parts.length !== 3) throw new Error('Invalid encrypted secret');
  const [iv, ciphertext, tag] = parts.map(part => Buffer.from(part, 'base64url'));
  const decipher = createDecipheriv('aes-256-gcm', encryptionKey(), iv);
  decipher.setAuthTag(tag);
  return Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString('utf8');
}

export function webhookSignature(rawBody: string, secret: string): string {
  return createHmac('sha256', secret).update(rawBody, 'utf8').digest('hex');
}
