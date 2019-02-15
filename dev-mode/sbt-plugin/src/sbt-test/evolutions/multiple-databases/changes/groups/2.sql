# Add another group

# --- !Ups

INSERT INTO groups VALUES (2, 'Group2');

# --- !Downs

DELETE FROM groups WHERE id = 2;
