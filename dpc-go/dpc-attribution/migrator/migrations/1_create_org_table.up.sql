CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE organization (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    version bigint DEFAULT 0,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    info jsonb NOT NULL
);