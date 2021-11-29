INSERT INTO public.avidb_message_format
VALUES (1, 'TAC', '2021-06-03 00:00:00');
INSERT INTO public.avidb_message_format
VALUES (2, 'IWXXM', '2021-06-03 00:00:00');

INSERT INTO public.avidb_message_routes
VALUES (1, 'TEST', 'Test Route', '2021-06-03 00:00:00');
VALUES (2, 'TEST2', 'Test Route 2', '2021-06-03 00:00:00');

INSERT INTO public.avidb_message_types
VALUES (1, 'METAR', 'metar', '2021-06-03 00:00:00', 1);
INSERT INTO public.avidb_message_types
VALUES (2, 'SPECI', 'speci', '2021-06-03 00:00:00', 1);
INSERT INTO public.avidb_message_types
VALUES (3, 'TAF', 'taf', '2021-06-03 00:00:00', 1);
INSERT INTO public.avidb_message_types
VALUES (4, 'SIGMET', 'sigmet', '2021-06-03 00:00:00', 1);
INSERT INTO public.avidb_message_types
VALUES (8, 'SWX', 'swx', '2021-06-03 00:00:00', 1);

INSERT INTO public.avidb_stations
VALUES (1, 'EFXX', 'EFXX Test Airport', NULL, 1, '1700-01-01 00:00:00', '9999-12-31 00:00:00', '2021-06-03 00:00:00', NULL,
        'US');
INSERT INTO public.avidb_stations
VALUES (2, 'YUDO', 'YUDO Test Airport', NULL, 1, '1700-01-01 00:00:00', '9999-12-31 00:00:00', '2021-06-03 00:00:00', NULL,
        'US');
INSERT INTO public.avidb_stations
VALUES (3, 'YUDD', 'YUDD Test Airport', NULL, 1, '1700-01-01 00:00:00', '9999-12-31 00:00:00', '2021-06-03 00:00:00', NULL,
        'US');
INSERT INTO public.avidb_stations
VALUES (4, 'EETN', 'EETN Test Airport', NULL, 1, '1700-01-01 00:00:00', '9999-12-31 00:00:00', '2021-06-03 00:00:00', NULL,
        'US');
INSERT INTO public.avidb_stations
VALUES (5, 'EFHA', 'EFKK Test Airport', NULL, 1, '1700-01-01 00:00:00', '9999-12-31 00:00:00', '2021-06-03 00:00:00', NULL,
        'US');
INSERT INTO public.avidb_stations
VALUES (6, 'EFKK', 'EFPO Test Airport', NULL, 1, '1700-01-01 00:00:00', '9999-12-31 00:00:00', '2021-06-03 00:00:00', NULL,
        'US');
INSERT INTO public.avidb_stations
VALUES (7, 'EFPO', 'EFJO Test Airport', NULL, 1, '1700-01-01 00:00:00', '9999-12-31 00:00:00', '2021-06-03 00:00:00', NULL,
        'US');

