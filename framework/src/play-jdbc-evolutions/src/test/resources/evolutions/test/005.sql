# --- Test data set

# --- !Ups

insert into test (id, name) values (95, 'Charlotte');

# --- !Downs

delete from test where name = 'Charlotte';
