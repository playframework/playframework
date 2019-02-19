# --- Test data set

# --- !Ups

insert into test (id, name) values (10, 'Sophia');

# --- !Downs

delete from test where name = 'Sophia';
