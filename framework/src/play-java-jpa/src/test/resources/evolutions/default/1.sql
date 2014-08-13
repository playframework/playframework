# --- Create database schema

# --- !Ups

create table TestEntity (
  id     bigint not null,
  name   varchar(255))
;

# --- !Downs

drop table if exists TestEntity;
