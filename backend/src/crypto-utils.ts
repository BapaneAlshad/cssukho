import { createHash, randomBytes, timingSafeEqual } from 'node:crypto';

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

