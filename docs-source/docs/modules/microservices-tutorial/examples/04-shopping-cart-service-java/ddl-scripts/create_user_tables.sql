--DROP TABLE IF EXISTS public.item_popularity;

CREATE TABLE IF NOT EXISTS public.item_popularity (
    itemid VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    count BIGINT NOT NULL, -- <1>
    PRIMARY KEY (itemid));
