# --- Sample dataset

# --- !Ups

insert into TestEntity (id, name) values (1, 'test1');

# --- !Downs

delete from TestEntity;
