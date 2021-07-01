BEGIN;

ALTER TABLE ONLY implementers
    ADD COLUMN ssas_group_id text;

ALTER TABLE ONLY implementer_org_relations
    ADD COLUMN ssas_system_id text;

COMMIT;