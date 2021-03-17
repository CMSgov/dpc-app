create table if not exists organizations
(
    id uuid not null
        constraint organizations_pkey
            primary key,
    id_system smallint not null,
    id_value varchar not null,
    organization_name varchar not null,
    address_use smallint not null,
    address_type smallint not null,
    line1 varchar not null,
    line2 varchar,
    city varchar,
    district varchar,
    state varchar,
    postal_code varchar,
    country varchar
);

create unique index if not exists organization_idx
    on organizations (id_system, id_value);

