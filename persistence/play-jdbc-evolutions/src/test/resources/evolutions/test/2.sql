# --- Test data set

# --- !Ups

insert into test (id, name) values (1, 'alice');
insert into test (id, name) values (2, 'bob');

# --- !Downs

delete from test;
