# --- Test database schema

# --- !Ups

create table test (id bigint not null, name varchar(255));
insert into test values (10, 'testing');

# --- !Downs

drop table if exists test;
