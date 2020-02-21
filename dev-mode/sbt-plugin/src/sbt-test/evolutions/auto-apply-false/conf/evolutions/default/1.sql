# Initial test schema

# --- !Ups

CREATE TABLE users (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    username varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO users(username) VALUES ('PlayerFromFirstEvolution');

# --- !Downs

DROP TABLE users;
