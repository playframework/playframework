# --- Test data set

# --- !Ups

insert into test (id, name) values (9, 'William');

# --- !Downs

delete from test where name = 'William';
