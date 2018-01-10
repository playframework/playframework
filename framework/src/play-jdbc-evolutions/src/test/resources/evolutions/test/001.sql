# --- !Ups

create table do_not_create_that_table (id bigint not null, name varchar(255));

# --- !Downs

drop table if exists do_not_create_that_table;
