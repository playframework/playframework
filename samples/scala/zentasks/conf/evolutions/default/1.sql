# --- First database schema

# --- !Ups

create table user (
  email                     varchar(255) not null primary key,
  name                      varchar(255) not null,
  password                  varchar(255) not null
);

create table project (
  id                        bigint not null primary key,
  name                      varchar(255) not null,
  folder                    varchar(255) not null
);

create sequence project_seq start with 1000;

create table project_member (
  project_id                bigint not null,
  user_email                varchar(255) not null,
  foreign key(project_id)   references project(id) on delete cascade,
  foreign key(user_email)   references user(email) on delete cascade
);

create table task (
  id                        bigint not null primary key,
  title                     varchar(255) not null,
  done                      boolean,
  due_date                  timestamp,
  assigned_to               varchar(255),
  project                   bigint not null,
  folder                    varchar(255),
  foreign key(assigned_to)  references user(email) on delete set null,
  foreign key(project)      references project(id) on delete cascade
);

create sequence task_seq start with 1000;

# --- !Downs

drop table if exists task;
drop sequence if exists task_seq;
drop table if exists project_member;
drop table if exists project;
drop sequence if exists project_seq;
drop table if exists user;
