# --- Test data set

# --- !Ups

insert into test (id, name) values (8, 'Liam');

# --- !Downs

delete from test where name = 'Liam';
