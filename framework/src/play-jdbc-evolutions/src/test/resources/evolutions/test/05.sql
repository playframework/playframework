# --- Test data set

# --- !Ups

insert into test (id, name) values (6, 'Noah');

# --- !Downs

delete from test where name = 'Noah';
