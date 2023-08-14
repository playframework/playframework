-- !Ups

CREATE TABLE table1 (
    column1 VARCHAR
);

INSERT INTO table1(column1) VALUES('hello');

-- !Downs

DROP TABLE table1;
