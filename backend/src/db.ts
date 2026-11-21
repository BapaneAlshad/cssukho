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
  return (await authorizedDevice(request)) !== null;
}

export async function authorizedDevice(request: Request): Promise<{ id: string } | null> {
  const token = tokenFromRequest(request);
  if (!token) return null;
  const rows = await db()`
    select id from sukho_private.admin_devices
    where token_hash = ${hashSecret(token)} and revoked_at is null
    limit 1
  `;
  if (rows.length !== 1) return null;
  await db()`update sukho_private.admin_devices set last_seen_at = now() where id = ${rows[0].id}`;
  return { id: rows[0].id as string };
}
