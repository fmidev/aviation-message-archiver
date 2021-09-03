CREATE SCHEMA IF NOT EXISTS public;

CREATE TABLE public.avidb_message_format
(
    format_id     smallint PRIMARY KEY NOT NULL,
    name          text,
    modified_last timestamp
);

CREATE TABLE public.avidb_message_routes
(
    route_id      int PRIMARY KEY NOT NULL,
    name          varchar(20),
    description   text,
    modified_last timestamp DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE public.avidb_message_types
(
    type_id       int PRIMARY KEY NOT NULL,
    type          varchar(20),
    description   text,
    modified_last timestamp DEFAULT CURRENT_TIMESTAMP(),
    iwxxm_flag    int
);

CREATE TABLE public.avidb_messages
(
    message_id     bigint auto_increment PRIMARY KEY NOT NULL,
    message_time   timestamp           NOT NULL,
    station_id     int                 NOT NULL,
    type_id        int                 NOT NULL,
    route_id       int                 NOT NULL,
    message        text                NOT NULL,
    valid_from     timestamp,
    valid_to       timestamp,
    created        timestamp DEFAULT CURRENT_TIMESTAMP(),
    file_modified  timestamp,
    flag           int       DEFAULT 0,
    messir_heading text,
    version        varchar(20),
    format_id      smallint  DEFAULT 1 NOT NULL
);

CREATE TABLE public.avidb_stations
(
    station_id    int PRIMARY KEY NOT NULL,
    icao_code     varchar(4),
    name          text,
    geom          geometry,
    elevation     int,
    valid_from    timestamp DEFAULT '1700-01-01 00:00:00',
    valid_to      timestamp DEFAULT '9999-12-31 00:00:00',
    modified_last timestamp DEFAULT CURRENT_TIMESTAMP(),
    iwxxm_flag    int,
    country_code  varchar(2)
);

CREATE TABLE public.avidb_message_iwxxm_details
(
    message_id         bigint PRIMARY KEY NOT NULL,
    collect_identifier text,
    iwxxm_version      text
);

CREATE
    UNIQUE INDEX avidb_stations_icao_code_key ON public.avidb_stations (icao_code);

CREATE
    INDEX avidb_stations_geom_idx ON public.avidb_stations (geom);

ALTER TABLE public.avidb_messages
    ADD CONSTRAINT avidb_messages_fk2
        FOREIGN KEY (type_id)
            REFERENCES public.avidb_message_types (type_id);

ALTER TABLE public.avidb_messages
    ADD CONSTRAINT avidb_messages_fk4
        FOREIGN KEY (format_id)
            REFERENCES public.avidb_message_format (format_id);

ALTER TABLE public.avidb_messages
    ADD CONSTRAINT avidb_messages_fk3
        FOREIGN KEY (route_id)
            REFERENCES public.avidb_message_routes (route_id);

ALTER TABLE public.avidb_messages
    ADD CONSTRAINT avidb_messages_fk1
        FOREIGN KEY (station_id)
            REFERENCES public.avidb_stations (station_id);

ALTER TABLE public.avidb_message_iwxxm_details
    ADD CONSTRAINT avidb_message_iwxxm_details_fk_message_id
        FOREIGN KEY (message_id)
            REFERENCES public.avidb_messages (message_id);

CREATE
    INDEX avidb_messages_new_station_id_idx ON public.avidb_messages (station_id);

CREATE
    INDEX avidb_messages_new_idx ON public.avidb_messages
    (
     message_time,
     type_id,
     station_id,
     format_id
        );

CREATE
    INDEX avidb_messages_new_created_idx ON public.avidb_messages (created);

CREATE TABLE public.avidb_rejected_messages
(
    icao_code      text,
    message_time   timestamp,
    type_id        int,
    route_id       int,
    message        text,
    valid_from     timestamp,
    valid_to       timestamp,
    created        timestamp DEFAULT CURRENT_TIMESTAMP(),
    file_modified  timestamp,
    flag           int       DEFAULT 0,
    messir_heading text,
    reject_reason  int,
    version        varchar(20)
);

CREATE
    INDEX avidb_rejected_messages_idx1 ON public.avidb_rejected_messages (created);

