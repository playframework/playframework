# --- Test data set

# --- !Ups

insert into test (id, name) values (96, 'Benjamin');

# --- !Downs

delete from test where name = 'Benjamin';
