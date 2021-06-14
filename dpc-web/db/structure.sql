SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: addresses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.addresses (
    id bigint NOT NULL,
    street character varying NOT NULL,
    street_2 character varying,
    city character varying NOT NULL,
    state character varying NOT NULL,
    zip character varying NOT NULL,
    addressable_type character varying NOT NULL,
    addressable_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    address_use integer DEFAULT 0 NOT NULL,
    address_type integer DEFAULT 0 NOT NULL
);


--
-- Name: addresses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.addresses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: addresses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.addresses_id_seq OWNED BY public.addresses.id;


--
-- Name: ar_internal_metadata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ar_internal_metadata (
    key character varying NOT NULL,
    value character varying,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: fhir_endpoints; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fhir_endpoints (
    id bigint NOT NULL,
    name character varying NOT NULL,
    status integer NOT NULL,
    uri character varying NOT NULL,
    organization_id integer,
    registered_organization_id integer
);


--
-- Name: fhir_endpoints_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fhir_endpoints_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fhir_endpoints_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fhir_endpoints_id_seq OWNED BY public.fhir_endpoints.id;


--
-- Name: internal_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.internal_users (
    id bigint NOT NULL,
    email character varying DEFAULT ''::character varying,
    encrypted_password character varying DEFAULT ''::character varying NOT NULL,
    sign_in_count integer DEFAULT 0 NOT NULL,
    current_sign_in_at timestamp without time zone,
    last_sign_in_at timestamp without time zone,
    current_sign_in_ip inet,
    last_sign_in_ip inet,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    provider character varying,
    uid character varying,
    name character varying,
    github_nickname character varying
);


--
-- Name: internal_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.internal_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: internal_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.internal_users_id_seq OWNED BY public.internal_users.id;


--
-- Name: old_passwords; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.old_passwords (
    id bigint NOT NULL,
    encrypted_password character varying NOT NULL,
    password_archivable_type character varying NOT NULL,
    password_archivable_id integer NOT NULL,
    password_salt character varying,
    created_at timestamp without time zone
);


--
-- Name: old_passwords_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.old_passwords_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: old_passwords_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.old_passwords_id_seq OWNED BY public.old_passwords.id;


--
-- Name: organization_user_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organization_user_assignments (
    id bigint NOT NULL,
    organization_id integer NOT NULL,
    user_id integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: organization_user_assignments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.organization_user_assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: organization_user_assignments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.organization_user_assignments_id_seq OWNED BY public.organization_user_assignments.id;


--
-- Name: organizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organizations (
    id bigint NOT NULL,
    name character varying NOT NULL,
    organization_type integer NOT NULL,
    num_providers integer DEFAULT 0,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    npi character varying,
    vendor character varying
);


--
-- Name: organizations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.organizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: organizations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.organizations_id_seq OWNED BY public.organizations.id;


--
-- Name: registered_organizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.registered_organizations (
    id bigint NOT NULL,
    organization_id integer NOT NULL,
    api_id character varying,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    api_endpoint_ref character varying,
    enabled boolean DEFAULT true NOT NULL
);


--
-- Name: registered_organizations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.registered_organizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: registered_organizations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.registered_organizations_id_seq OWNED BY public.registered_organizations.id;


--
-- Name: schema_migrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schema_migrations (
    version character varying NOT NULL
);


--
-- Name: taggings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.taggings (
    id bigint NOT NULL,
    tag_id integer,
    taggable_type character varying,
    taggable_id bigint,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: taggings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.taggings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: taggings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.taggings_id_seq OWNED BY public.taggings.id;


--
-- Name: tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tags (
    id bigint NOT NULL,
    name character varying,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: tags_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tags_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tags_id_seq OWNED BY public.tags.id;


--
-- Name: the_resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.the_resources (
    id bigint NOT NULL,
    password_changed_at timestamp without time zone
);


--
-- Name: the_resources_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.the_resources_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: the_resources_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.the_resources_id_seq OWNED BY public.the_resources.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    first_name character varying NOT NULL,
    last_name character varying NOT NULL,
    requested_organization character varying NOT NULL,
    requested_organization_type integer NOT NULL,
    address_1 character varying NOT NULL,
    address_2 character varying DEFAULT ''::character varying,
    city character varying NOT NULL,
    state character varying NOT NULL,
    zip character varying NOT NULL,
    agree_to_terms boolean NOT NULL,
    email character varying DEFAULT ''::character varying NOT NULL,
    encrypted_password character varying DEFAULT ''::character varying NOT NULL,
    sign_in_count integer DEFAULT 0 NOT NULL,
    current_sign_in_at timestamp without time zone,
    last_sign_in_at timestamp without time zone,
    current_sign_in_ip inet,
    last_sign_in_ip inet,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    requested_num_providers integer DEFAULT 0,
    reset_password_token character varying,
    reset_password_sent_at timestamp without time zone,
    confirmation_token character varying,
    confirmed_at timestamp without time zone,
    confirmation_sent_at timestamp without time zone,
    password_changed_at timestamp without time zone
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: addresses id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.addresses ALTER COLUMN id SET DEFAULT nextval('public.addresses_id_seq'::regclass);


--
-- Name: fhir_endpoints id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fhir_endpoints ALTER COLUMN id SET DEFAULT nextval('public.fhir_endpoints_id_seq'::regclass);


--
-- Name: internal_users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.internal_users ALTER COLUMN id SET DEFAULT nextval('public.internal_users_id_seq'::regclass);


--
-- Name: old_passwords id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.old_passwords ALTER COLUMN id SET DEFAULT nextval('public.old_passwords_id_seq'::regclass);


--
-- Name: organization_user_assignments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization_user_assignments ALTER COLUMN id SET DEFAULT nextval('public.organization_user_assignments_id_seq'::regclass);


--
-- Name: organizations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations ALTER COLUMN id SET DEFAULT nextval('public.organizations_id_seq'::regclass);


--
-- Name: registered_organizations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registered_organizations ALTER COLUMN id SET DEFAULT nextval('public.registered_organizations_id_seq'::regclass);


--
-- Name: taggings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taggings ALTER COLUMN id SET DEFAULT nextval('public.taggings_id_seq'::regclass);


--
-- Name: tags id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tags ALTER COLUMN id SET DEFAULT nextval('public.tags_id_seq'::regclass);


--
-- Name: the_resources id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.the_resources ALTER COLUMN id SET DEFAULT nextval('public.the_resources_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: addresses addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT addresses_pkey PRIMARY KEY (id);


--
-- Name: ar_internal_metadata ar_internal_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ar_internal_metadata
    ADD CONSTRAINT ar_internal_metadata_pkey PRIMARY KEY (key);


--
-- Name: fhir_endpoints fhir_endpoints_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fhir_endpoints
    ADD CONSTRAINT fhir_endpoints_pkey PRIMARY KEY (id);


--
-- Name: internal_users internal_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.internal_users
    ADD CONSTRAINT internal_users_pkey PRIMARY KEY (id);


--
-- Name: old_passwords old_passwords_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.old_passwords
    ADD CONSTRAINT old_passwords_pkey PRIMARY KEY (id);


--
-- Name: organization_user_assignments organization_user_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization_user_assignments
    ADD CONSTRAINT organization_user_assignments_pkey PRIMARY KEY (id);


--
-- Name: organizations organizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);


--
-- Name: registered_organizations registered_organizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registered_organizations
    ADD CONSTRAINT registered_organizations_pkey PRIMARY KEY (id);


--
-- Name: schema_migrations schema_migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schema_migrations
    ADD CONSTRAINT schema_migrations_pkey PRIMARY KEY (version);


--
-- Name: taggings taggings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taggings
    ADD CONSTRAINT taggings_pkey PRIMARY KEY (id);


--
-- Name: tags tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tags
    ADD CONSTRAINT tags_pkey PRIMARY KEY (id);


--
-- Name: the_resources the_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.the_resources
    ADD CONSTRAINT the_resources_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: index_addresses_on_addressable_type_and_addressable_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_addresses_on_addressable_type_and_addressable_id ON public.addresses USING btree (addressable_type, addressable_id);


--
-- Name: index_fhir_endpoints_on_registered_organization_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_fhir_endpoints_on_registered_organization_id ON public.fhir_endpoints USING btree (registered_organization_id);


--
-- Name: index_internal_users_on_uid_and_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_internal_users_on_uid_and_provider ON public.internal_users USING btree (uid, provider);


--
-- Name: index_org_user_assignments_on_organization_id_and_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_org_user_assignments_on_organization_id_and_user_id ON public.organization_user_assignments USING btree (organization_id, user_id);


--
-- Name: index_password_archivable; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_password_archivable ON public.old_passwords USING btree (password_archivable_type, password_archivable_id);


--
-- Name: index_registered_organizations_on_organization_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_registered_organizations_on_organization_id ON public.registered_organizations USING btree (organization_id);


--
-- Name: index_taggings_on_taggable_type_and_taggable_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_taggings_on_taggable_type_and_taggable_id ON public.taggings USING btree (taggable_type, taggable_id);


--
-- Name: index_the_resources_on_password_changed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_the_resources_on_password_changed_at ON public.the_resources USING btree (password_changed_at);


--
-- Name: index_users_on_confirmation_token; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_users_on_confirmation_token ON public.users USING btree (confirmation_token);


--
-- Name: index_users_on_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_users_on_created_at ON public.users USING btree (created_at);


--
-- Name: index_users_on_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_users_on_email ON public.users USING btree (email);


--
-- Name: index_users_on_last_name_and_first_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_users_on_last_name_and_first_name ON public.users USING btree (last_name, first_name);


--
-- Name: index_users_on_requested_organization; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_users_on_requested_organization ON public.users USING btree (requested_organization);


--
-- Name: index_users_on_reset_password_token; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_users_on_reset_password_token ON public.users USING btree (reset_password_token);


--
-- PostgreSQL database dump complete
--

SET search_path TO "$user", public;

INSERT INTO "schema_migrations" (version) VALUES
('20190717172018'),
('20190718151153'),
('20190801183311'),
('20190822164911'),
('20190822174930'),
('20190827134448'),
('20190827164559'),
('20190903190016'),
('20190903190849'),
('20190904163549'),
('20190904204127'),
('20190911175348'),
('20190916190939'),
('20190918195816'),
('20190918195851'),
('20191007160000'),
('20191007160244'),
('20191007162013'),
('20191017201830'),
('20191022185034'),
('20191024190829'),
('20191028184945'),
('20191028190113'),
('20191030141914'),
('20191101134050'),
('20191120215051'),
('20200123163410'),
('20200124204655'),
('20200127141604'),
('20200131173905'),
('20200204135210'),
('20200204190846'),
('20200226152104'),
('20200226152127'),
('20200402165857'),
('20200410125053'),
('20200427124335'),
('20200427154230'),
('20200519213700'),
('20200624164349'),
('20200709181216'),
('20200715203040'),
('20200909131201'),
('20200909150413'),
('20200909153523');


