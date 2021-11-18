CREATE TABLE "implementer_org_relations" (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    implementer_id uuid NOT NULL,
    organization_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    enabled_on timestamp with time zone,
    deleted_at timestamp with time zone,
    status int DEFAULT 1,
    CONSTRAINT implementer_org_relation_unique UNIQUE (implementer_id, organization_id)
);
