# Add another user

# --- !Ups

INSERT INTO users VALUES (2, 'Player2');

# --- !Downs

DELETE FROM users WHERE id = 2;
