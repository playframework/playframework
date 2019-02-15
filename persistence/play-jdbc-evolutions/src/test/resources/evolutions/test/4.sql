# --- !Ups

insert into test (id, name) values (5, 'Emma');

# --- !Downs

delete from test where name = 'Emma';
