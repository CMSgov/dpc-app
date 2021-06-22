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


--
-- Name: admins; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.admins (
    id bigint NOT NULL,
    provider character varying,
    uid character varying,
    name character varying,
    github_nickname character varying,
    email character varying DEFAULT ''::character varying,
    encrypted_password character varying DEFAULT ''::character varying NOT NULL,
    sign_in_count integer DEFAULT 0 NOT NULL,
    current_sign_in_at timestamp without time zone,
    last_sign_in_at timestamp without time zone,
    current_sign_in_ip character varying,
    last_sign_in_ip character varying,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: admins_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.admins_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admins_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.admins_id_seq OWNED BY public.admins.id;


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
-- Name: schema_migrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schema_migrations (
    version character varying NOT NULL
);


--
-- Name: the_resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.the_resources (
    id bigint NOT NULL,
    password_changed_at timestamp without time zone,
    last_activity_at timestamp without time zone,
    expired_at timestamp without time zone
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
    implementer character varying NOT NULL,
    agree_to_terms boolean,
    email character varying DEFAULT ''::character varying NOT NULL,
    encrypted_password character varying DEFAULT ''::character varying NOT NULL,
    reset_password_token character varying,
    reset_password_sent_at timestamp without time zone,
    password_changed_at timestamp without time zone,
    sign_in_count integer DEFAULT 0 NOT NULL,
    current_sign_in_at timestamp without time zone,
    last_sign_in_at timestamp without time zone,
    current_sign_in_ip character varying,
    last_sign_in_ip character varying,
    confirmation_token character varying,
    confirmed_at timestamp without time zone,
    confirmation_sent_at timestamp without time zone,
    last_activity_at timestamp without time zone,
    expired_at timestamp without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    invitation_token character varying,
    invitation_created_at timestamp without time zone,
    invitation_sent_at timestamp without time zone,
    invitation_accepted_at timestamp without time zone,
    invitation_limit integer,
    invited_by_type character varying,
    invited_by_id bigint,
    invitations_count integer DEFAULT 0,
    implementer_id character varying
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
-- Name: admins id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admins ALTER COLUMN id SET DEFAULT nextval('public.admins_id_seq'::regclass);


--
-- Name: old_passwords id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.old_passwords ALTER COLUMN id SET DEFAULT nextval('public.old_passwords_id_seq'::regclass);


--
-- Name: the_resources id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.the_resources ALTER COLUMN id SET DEFAULT nextval('public.the_resources_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: admins admins_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_pkey PRIMARY KEY (id);


--
-- Name: ar_internal_metadata ar_internal_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ar_internal_metadata
    ADD CONSTRAINT ar_internal_metadata_pkey PRIMARY KEY (key);


--
-- Name: old_passwords old_passwords_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.old_passwords
    ADD CONSTRAINT old_passwords_pkey PRIMARY KEY (id);


--
-- Name: schema_migrations schema_migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schema_migrations
    ADD CONSTRAINT schema_migrations_pkey PRIMARY KEY (version);


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
-- Name: index_admins_on_uid_and_provider; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_admins_on_uid_and_provider ON public.admins USING btree (uid, provider);


--
-- Name: index_password_archivable; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_password_archivable ON public.old_passwords USING btree (password_archivable_type, password_archivable_id);


--
-- Name: index_the_resources_on_expired_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_the_resources_on_expired_at ON public.the_resources USING btree (expired_at);


--
-- Name: index_the_resources_on_last_activity_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_the_resources_on_last_activity_at ON public.the_resources USING btree (last_activity_at);


--
-- Name: index_the_resources_on_password_changed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_the_resources_on_password_changed_at ON public.the_resources USING btree (password_changed_at);


--
-- Name: index_users_on_confirmation_token; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_users_on_confirmation_token ON public.users USING btree (confirmation_token);


--
-- Name: index_users_on_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_users_on_email ON public.users USING btree (email);


--
-- Name: index_users_on_implementer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_users_on_implementer ON public.users USING btree (implementer);


--
-- Name: index_users_on_invitation_token; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_users_on_invitation_token ON public.users USING btree (invitation_token);


--
-- Name: index_users_on_invited_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_users_on_invited_by ON public.users USING btree (invited_by_type, invited_by_id);


--
-- Name: index_users_on_invited_by_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX index_users_on_invited_by_id ON public.users USING btree (invited_by_id);


--
-- Name: index_users_on_reset_password_token; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX index_users_on_reset_password_token ON public.users USING btree (reset_password_token);


--
-- PostgreSQL database dump complete
--

SET search_path TO "$user", public;

INSERT INTO "schema_migrations" (version) VALUES
('20210409181131'),
('20210423155520'),
('20210423155611'),
('20210429133701'),
('20210430160219'),
('20210617195159');


