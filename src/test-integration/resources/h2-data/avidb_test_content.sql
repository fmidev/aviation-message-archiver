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
VALUES (2, 'TAF', 'taf', '2021-06-03 00:00:00', 1);

INSERT INTO public.avidb_stations
VALUES (1, 'EFXX', 'Test Airport', NULL, 1, '1700-01-01 00:00:00', '9999-12-31 00:00:00', '2021-06-03 00:00:00', NULL,
        'US');