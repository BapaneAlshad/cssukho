import { randomBytes } from 'node:crypto';
import { hashSecret } from './crypto-utils.js';

export const INSTAGRAM_PLATFORM = 'instagram' as const;
export const DEFAULT_INSTAGRAM_SCOPES = [
  'instagram_business_basic',
  'instagram_business_manage_comments',
  'instagram_business_manage_messages',
];

export type InstagramConfig = {
  appId: string;
  appSecret: string;
  redirectUri: string;
  authorizeUrl: string;
  tokenUrl: string;
  graphUrl: string;
  apiVersion: string;
  scopes: string[];
  webhookVerifyToken: string;
};

export function instagramConfig(): InstagramConfig | null {
  const appId = process.env.INSTAGRAM_APP_ID?.trim();
  const appSecret = process.env.INSTAGRAM_APP_SECRET?.trim();
  const redirectUri = process.env.INSTAGRAM_REDIRECT_URI?.trim();
  const webhookVerifyToken = process.env.INSTAGRAM_WEBHOOK_VERIFY_TOKEN?.trim();
  if (!appId || !appSecret || !redirectUri || !webhookVerifyToken) return null;
  return {
    appId,
    appSecret,
    redirectUri,
    authorizeUrl: process.env.INSTAGRAM_AUTHORIZE_URL?.trim() || 'https://www.instagram.com/oauth/authorize',
    tokenUrl: process.env.INSTAGRAM_TOKEN_URL?.trim() || 'https://api.instagram.com/oauth/access_token',
    graphUrl: (process.env.INSTAGRAM_GRAPH_URL?.trim() || 'https://graph.instagram.com').replace(/\/$/, ''),
    apiVersion: process.env.INSTAGRAM_API_VERSION?.trim() || 'v27.0',
    scopes: (process.env.INSTAGRAM_SCOPES || DEFAULT_INSTAGRAM_SCOPES.join(','))
      .split(',').map(scope => scope.trim()).filter(Boolean),
    webhookVerifyToken,
  };
}

export function newOAuthState(): string {
  return randomBytes(32).toString('base64url');
}

export function oauthStateHash(state: string): string {
  return hashSecret(state);
}

export function instagramGraphEndpoint(config: InstagramConfig, path: string): string {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  return `${config.graphUrl}/${config.apiVersion}${normalized}`;
}

export function htmlResult(title: string, message: string): Response {
  const escape = (value: string) => value.replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[character] ?? character));
  return new Response(`<!doctype html><html lang="id"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escape(title)}</title><body style="font-family:system-ui,sans-serif;padding:32px;color:#17324d"><h1>${escape(title)}</h1><p>${escape(message)}</p><p>Anda dapat kembali ke aplikasi SUKHO RAMEN.</p></body></html>`, {
    status: 200,
    headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store' },
  });
}
