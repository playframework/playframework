# --- Test data set

# --- !Ups

insert into test (id, name) values (94, 'Jacob');

# --- !Downs

delete from test where name = 'Jacob';
