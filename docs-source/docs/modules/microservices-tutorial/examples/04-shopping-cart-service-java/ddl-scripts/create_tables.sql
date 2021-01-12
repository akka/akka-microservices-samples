--DROP TABLE IF EXISTS public.event_journal;

CREATE TABLE IF NOT EXISTS public.event_journal(
  ordering BIGSERIAL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  deleted BOOLEAN DEFAULT FALSE NOT NULL,

  writer VARCHAR(255) NOT NULL,
  write_timestamp BIGINT,
  adapter_manifest VARCHAR(255),

  event_ser_id INTEGER NOT NULL,
  event_ser_manifest VARCHAR(255) NOT NULL,
  event_payload BYTEA NOT NULL,

  meta_ser_id INTEGER,
  meta_ser_manifest VARCHAR(255),
  meta_payload BYTEA,

  PRIMARY KEY(persistence_id, sequence_number)
);

CREATE UNIQUE INDEX event_journal_ordering_idx ON public.event_journal(ordering);

--DROP TABLE IF EXISTS public.event_tag;

CREATE TABLE IF NOT EXISTS public.event_tag(
    event_id BIGINT,
    tag VARCHAR(256),
    PRIMARY KEY(event_id, tag),
    CONSTRAINT fk_event_journal
      FOREIGN KEY(event_id)
      REFERENCES event_journal(ordering)
      ON DELETE CASCADE
);

--DROP TABLE IF EXISTS public.snapshot;

CREATE TABLE IF NOT EXISTS public.snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,

  snapshot_ser_id INTEGER NOT NULL,
  snapshot_ser_manifest VARCHAR(255) NOT NULL,
  snapshot_payload BYTEA NOT NULL,

  meta_ser_id INTEGER,
  meta_ser_manifest VARCHAR(255),
  meta_payload BYTEA,

  PRIMARY KEY(persistence_id, sequence_number)
);

--drop table if exists public."AKKA_PROJECTION_OFFSET_STORE";

create table if not exists public."AKKA_PROJECTION_OFFSET_STORE" (
    "PROJECTION_NAME" VARCHAR(255) NOT NULL,
    "PROJECTION_KEY" VARCHAR(255) NOT NULL,
    "CURRENT_OFFSET" VARCHAR(255) NOT NULL,
    "MANIFEST" VARCHAR(4) NOT NULL,
    "MERGEABLE" BOOLEAN NOT NULL,
    "LAST_UPDATED" BIGINT NOT NULL,
    primary key("PROJECTION_NAME","PROJECTION_KEY"));

create index if not exists "PROJECTION_NAME_INDEX" on public."AKKA_PROJECTION_OFFSET_STORE" ("PROJECTION_NAME");
