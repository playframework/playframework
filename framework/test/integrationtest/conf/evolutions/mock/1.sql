# Mock schema

# --- !Ups

CREATE TABLE Mock (
    id integer NOT NULL AUTO_INCREMENT,
    value varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE Mock;