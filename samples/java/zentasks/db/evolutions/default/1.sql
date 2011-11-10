
# --- !Ups

create table user (
  email                     varchar(255) not null,
  name                      varchar(255),
  password                  varchar(255),
  constraint pk_user primary key (email)
);

create table project (
  id                        bigint not null,
  name                      varchar(255),
  folder                    varchar(255),
  constraint pk_project primary key (id)
);

create table task (
  id                        bigint not null,
  title                     varchar(255),
  done                      boolean,
  due_date                  timestamp,
  assigned_to_email         varchar(255),
  folder                    varchar(255),
  project_id                bigint,
  constraint pk_task primary key (id),
  foreign key (assigned_to_email) references user (email) on delete set null on update restrict,
  foreign key (project_id) references project (id) on delete cascade on update restrict
);

create table project_user (
  project_id                     bigint not null,
  user_email                     varchar(255) not null,
  constraint pk_project_user primary key (project_id, user_email),
  foreign key (project_id) references project (id) on delete cascade on update restrict,
  foreign key (user_email) references user (email) on delete cascade on update restrict
);

create sequence project_seq start with 1000;
create sequence task_seq start with 1000;

# --- !Downs

drop table if exists task;
drop table if exists project_user;
drop table if exists project;
drop table if exists user;

drop sequence if exists project_seq;
drop sequence if exists task_seq;


