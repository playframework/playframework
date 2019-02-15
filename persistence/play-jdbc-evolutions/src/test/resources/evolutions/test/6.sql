# --- Test data set

# --- !Ups

insert into test (id, name) values (7, 'Olivia');

# --- !Downs

delete from test where name = 'Olivia';
