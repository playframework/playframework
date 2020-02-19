# Initial test schema

# --- !Ups

INSERT INTO users(username) VALUES ('PlayerFromThirdEvolution');


WRONG COMMAND TO FAIL THE MIGRATION

# --- !Downs

DELETE FROM users where id = 3;
