# Initial test schema

# --- !Ups

INSERT INTO users VALUES (3, 'Player3');


WRONG COMMAND TO FAIL THE MIGRATION

# --- !Downs

DELETE FROM users where id = 3;
