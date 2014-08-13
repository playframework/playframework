# --- Test database schema

# --- !Ups

create table test (id bigint not null, name varchar(255));

# --- !Downs

drop table if exists test;
