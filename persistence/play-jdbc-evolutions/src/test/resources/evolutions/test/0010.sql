# --- Test data set

# --- !Ups

insert into test (id, name) values (99, 'Isabella');

# --- !Downs

delete from test where name = 'Isabella';
