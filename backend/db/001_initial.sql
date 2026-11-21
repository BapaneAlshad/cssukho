create schema if not exists sukho_private;
revoke all on schema sukho_private from public, anon, authenticated;

create table if not exists sukho_private.activation_codes (
    code_hash text primary key,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    used_at timestamptz
);

create table if not exists sukho_private.admin_devices (
    id uuid primary key,
    token_hash text not null unique,
    activation_code_hash text not null references sukho_private.activation_codes(code_hash),
    created_at timestamptz not null default now(),
    revoked_at timestamptz,
    last_seen_at timestamptz
);
create unique index if not exists one_active_admin_device
    on sukho_private.admin_devices ((true)) where revoked_at is null;

create table if not exists sukho_private.platform_connections (
    platform text primary key check (platform in ('instagram', 'tiktok')),
    status text not null check (status in ('disconnected', 'pending_access', 'connected', 'reauth_required', 'unsupported')),
    handle text,
    can_read boolean not null default false,
    can_reply boolean not null default false,
    updated_at timestamptz not null default now()
);
insert into sukho_private.platform_connections (platform, status)
values ('instagram', 'disconnected'), ('tiktok', 'pending_access')
on conflict (platform) do nothing;

create table if not exists sukho_private.interactions (
    id uuid primary key default gen_random_uuid(),
    platform text not null references sukho_private.platform_connections(platform),
    provider_account_id text not null,
    kind text not null check (kind in ('dm', 'comment')),
    provider_key text not null,
    sender text not null,
    handle text not null default '',
    initials text not null default '',
    post_caption text,
    post_kind text,
    post_url text,
    source_url text,
    reply_restriction text,
    completed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (platform, provider_account_id, kind, provider_key)
);
create index if not exists interactions_recent on sukho_private.interactions (updated_at desc, id desc);

create table if not exists sukho_private.messages (
    id uuid primary key default gen_random_uuid(),
    interaction_id uuid not null references sukho_private.interactions(id) on delete cascade,
    provider_message_key text,
    request_id uuid,
    direction text not null check (direction in ('incoming', 'outgoing')),
    body text not null,
    status text not null check (status in ('sending', 'sent', 'failed')),
    error text,
    occurred_at timestamptz not null,
    created_at timestamptz not null default now()
);
create unique index if not exists messages_provider_unique
    on sukho_private.messages (interaction_id, provider_message_key) where provider_message_key is not null;
create unique index if not exists messages_request_unique
    on sukho_private.messages (request_id) where request_id is not null;
create index if not exists messages_thread_time
    on sukho_private.messages (interaction_id, occurred_at, id);

