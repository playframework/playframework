# Add another user

# --- !Ups

INSERT INTO users(username) VALUES ('PlayerFromSecondEvolution');

# --- !Downs

DELETE FROM users WHERE username = 'PlayerFromSecondEvolution';
