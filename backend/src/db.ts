import postgres from 'postgres';
import { hashSecret, tokenFromRequest } from './crypto-utils.js';

let client: ReturnType<typeof postgres> | undefined;

export function db(): ReturnType<typeof postgres> {
  if (!client) {
    const url = process.env.DATABASE_URL;
    if (!url) throw new Error('DATABASE_URL is required');
    client = postgres(url, { max: 1, prepare: false, ssl: 'require' });
  }
  return client;
}

export async function authorize(request: Request): Promise<boolean> {
  const token = tokenFromRequest(request);
  if (!token) return false;
  const rows = await db()`
    select id from sukho_private.admin_devices
    where token_hash = ${hashSecret(token)} and revoked_at is null
    limit 1
  `;
  return rows.length === 1;
}

