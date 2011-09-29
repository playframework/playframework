# --- Generated

# --- !Ups
CREATE TABLE "tasks" ("id" BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"name" VARCHAR NOT NULL,"due_date" DATE NOT NULL,"done" BOOLEAN NOT NULL)

# --- !Downs
DROP TABLE "tasks"

