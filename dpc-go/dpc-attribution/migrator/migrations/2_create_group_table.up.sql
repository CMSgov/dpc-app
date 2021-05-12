CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE "group" (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    organization_id uuid NOT NULL,
    version bigint DEFAULT 0,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    info jsonb NOT NULL,
    CONSTRAINT fk_organization
        FOREIGN KEY(organization_id)
            REFERENCES organization(id)
);