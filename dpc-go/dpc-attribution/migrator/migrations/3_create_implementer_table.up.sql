CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE "implementers" (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    name varchar(200) NOT NULL,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    deleted_at timestamp with time zone
);