create table if not exists sukho_private.oauth_states (
    state_hash text primary key,
    platform text not null references sukho_private.platform_connections(platform),
    device_id uuid not null references sukho_private.admin_devices(id),
    redirect_uri text not null,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);
create index if not exists oauth_states_expiry on sukho_private.oauth_states (expires_at);
create index if not exists oauth_states_platform on sukho_private.oauth_states (platform);
create index if not exists oauth_states_device on sukho_private.oauth_states (device_id);

create table if not exists sukho_private.platform_tokens (
    platform text primary key references sukho_private.platform_connections(platform),
    provider_account_id text not null,
    handle text,
    access_token_ciphertext text not null,
    token_expires_at timestamptz,
    scopes text[] not null default '{}',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists sukho_private.provider_events (
    platform text not null references sukho_private.platform_connections(platform),
    provider_event_key text not null,
    received_at timestamptz not null default now(),
    payload jsonb not null,
    primary key (platform, provider_event_key)
);

create index if not exists admin_devices_activation_code on sukho_private.admin_devices (activation_code_hash);

revoke all on table sukho_private.oauth_states from public, anon, authenticated;
revoke all on table sukho_private.platform_tokens from public, anon, authenticated;
revoke all on table sukho_private.provider_events from public, anon, authenticated;

alter table sukho_private.oauth_states enable row level security;
alter table sukho_private.platform_tokens enable row level security;
alter table sukho_private.provider_events enable row level security;
