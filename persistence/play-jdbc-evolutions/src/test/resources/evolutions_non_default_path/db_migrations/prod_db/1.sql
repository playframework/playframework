# --- Test database schema

# --- !Ups

create table test (id bigint not null, name varchar(255));
insert into test values (9, 'foobar');

# --- !Downs

drop table if exists test;
