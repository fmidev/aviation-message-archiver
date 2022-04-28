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

--
-- Data for Name: avidb_message_types; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.avidb_message_types
VALUES (1, 'METAR', 'Aerodrome routine meteorological report', DEFAULT, NULL);
INSERT INTO public.avidb_message_types
VALUES (2, 'SPECI', 'Aerodrome special meteorological report', DEFAULT, NULL);
INSERT INTO public.avidb_message_types
VALUES (3, 'TAF', 'Aerodrome forecast', DEFAULT, NULL);
INSERT INTO public.avidb_message_types
VALUES (4, 'SIGMET', 'Significant meteorological information', DEFAULT, NULL);
INSERT INTO public.avidb_message_types
VALUES (5, 'AIRMET', 'Airmen''s meteorological information', DEFAULT, NULL);
INSERT INTO public.avidb_message_types
VALUES (6, 'VAA', 'Volcanic ash advisory', DEFAULT, NULL);
INSERT INTO public.avidb_message_types
VALUES (7, 'TCA', 'Tropical cyclone advisory', DEFAULT, NULL);
INSERT INTO public.avidb_message_types
VALUES (8, 'SWX', 'Space weather advisory', DEFAULT, NULL);
