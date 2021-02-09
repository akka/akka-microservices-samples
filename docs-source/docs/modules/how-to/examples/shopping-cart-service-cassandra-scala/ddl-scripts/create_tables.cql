-- This CQL script will create the keyspace and all tables needed for the this sample. 
-- It includes the messages and snapshot tables (write-side) and the projection tables (read-side).

-- NOTE: the keyspace as created here is probably not what you need in a production environment. 
-- This is good enough for local development though. 

CREATE KEYSPACE IF NOT EXISTS shoppingcartservice
  WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };

USE shoppingcartservice;

CREATE TABLE IF NOT EXISTS messages (
  persistence_id text,
  partition_nr bigint,
  sequence_nr bigint,
  timestamp timeuuid,
  timebucket text,
  writer_uuid text,
  ser_id int,
  ser_manifest text,
  event_manifest text,
  event blob,
  meta_ser_id int,
  meta_ser_manifest text,
  meta blob,
  tags set<text>,
  PRIMARY KEY ((persistence_id, partition_nr), sequence_nr, timestamp));

CREATE TABLE IF NOT EXISTS tag_views (
  tag_name text,
  persistence_id text,
  sequence_nr bigint,
  timebucket bigint,
  timestamp timeuuid,
  tag_pid_sequence_nr bigint,
  writer_uuid text,
  ser_id int,
  ser_manifest text,
  event_manifest text,
  event blob,
  meta_ser_id int,
  meta_ser_manifest text,
  meta blob,
  PRIMARY KEY ((tag_name, timebucket), timestamp, persistence_id, tag_pid_sequence_nr));

CREATE TABLE IF NOT EXISTS tag_write_progress(
  persistence_id text,
  tag text,
  sequence_nr bigint,
  tag_pid_sequence_nr bigint,
  offset timeuuid,
  PRIMARY KEY (persistence_id, tag));

CREATE TABLE IF NOT EXISTS tag_scanning(
  persistence_id text,
  sequence_nr bigint,
  PRIMARY KEY (persistence_id));

CREATE TABLE IF NOT EXISTS metadata(
  persistence_id text PRIMARY KEY,
  deleted_to bigint,
  properties map<text,text>);

CREATE TABLE IF NOT EXISTS all_persistence_ids(
  persistence_id text PRIMARY KEY);

CREATE TABLE IF NOT EXISTS snapshots (
  persistence_id text,
  sequence_nr bigint,
  timestamp bigint,
  ser_id int,
  ser_manifest text,
  snapshot_data blob,
  snapshot blob,
  meta_ser_id int,
  meta_ser_manifest text,
  meta blob,
  PRIMARY KEY (persistence_id, sequence_nr))
  WITH CLUSTERING ORDER BY (sequence_nr DESC);

CREATE TABLE IF NOT EXISTS offset_store (
  projection_name text,
  partition int,
  projection_key text,
  offset text,
  manifest text,
  last_updated timestamp,
  PRIMARY KEY ((projection_name, partition), projection_key));
