# Initial test schema

# --- !Ups

INSERT INTO users VALUES (3, 'Player3');

WRONG

# --- !Downs

DELETE FROM users where id = 3;
