export function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' },
  });
}

export function error(code: string, message: string, status: number): Response {
  return json({ code, message }, status);
}

export async function readBody(request: Request): Promise<Record<string, unknown> | null> {
  if (Number(request.headers.get('content-length') ?? 0) > 8192) return null;
  try {
    const raw = await request.text();
    if (raw.length > 8192) return null;
    const parsed: unknown = JSON.parse(raw);
    return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed as Record<string, unknown> : null;
  } catch {
    return null;
  }
}

export function failed(cause: unknown): Response {
  // Never return SQL details or secrets to the app.
  return error('server_error', 'Layanan sedang bermasalah. Coba lagi.', 500);
}

