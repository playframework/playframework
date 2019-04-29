# --- Test data set

# --- !Ups

insert into test (id, name) values (98, 'James');

# --- !Downs

delete from test where name = 'James';
