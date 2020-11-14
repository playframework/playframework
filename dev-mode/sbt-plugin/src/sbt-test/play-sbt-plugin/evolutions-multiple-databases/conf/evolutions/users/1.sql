# Initial test schema for users db

# --- !Ups

CREATE TABLE users (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    username varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO users VALUES (1, 'Player1');

# --- !Downs

DROP TABLE users;
