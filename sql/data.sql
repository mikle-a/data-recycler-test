INSERT INTO "data".big_table (ts)
SELECT now() FROM generate_series(1,10000000);