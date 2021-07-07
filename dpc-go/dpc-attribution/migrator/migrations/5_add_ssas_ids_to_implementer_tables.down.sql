BEGIN;

ALTER TABLE ONLY implementers
    drop COLUMN if exists ssas_group_id;

ALTER TABLE ONLY implementer_org_relations
    drop COLUMN if exists ssas_system_id;

COMMIT;