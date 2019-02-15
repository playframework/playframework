# --- Test data set

# --- !Ups

insert into test (id, name) values (97, 'Mia');

# --- !Downs

delete from test where name = 'Mia';
