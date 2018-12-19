# Initial test schema for groups db

# --- !Ups

CREATE TABLE groups (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO groups VALUES (1, 'Group1');

# --- !Downs

DROP TABLE groups;
