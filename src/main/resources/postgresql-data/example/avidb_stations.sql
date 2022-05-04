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
-- Data for Name: avidb_stations; Type: TABLE DATA; Schema: public; Owner: -
--

-- NOTE: The ST_Point() function does not work during initialization; rows using the function need to be imported manually.
-- Example row:
-- INSERT INTO public.avidb_stations
-- VALUES (1, 'YUDO', 'Donlon Aerodrome', ST_SetSRID(ST_Point(0.0, 0.0), 4326), 0, DEFAULT, DEFAULT, DEFAULT, NULL, 'XX');
