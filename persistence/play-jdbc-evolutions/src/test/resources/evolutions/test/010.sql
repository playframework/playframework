# --- Test data set

# --- !Ups

insert into test (id, name) values (11, 'Mason');

# --- !Downs

delete from test where name = 'Mason';
