
# --- !Ups

create table account (
  email                     varchar(255) not null,
  name                      varchar(255),
  password                  varchar(255),
  constraint pk_account primary key (email)
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
  foreign key (assigned_to_email) references account (email) on delete set null on update restrict,
  foreign key (project_id) references project (id) on delete cascade on update restrict
);

create table project_account (
  project_id                     bigint not null,
  account_email                     varchar(255) not null,
  constraint pk_project_account primary key (project_id, account_email),
  foreign key (project_id) references project (id) on delete cascade on update restrict,
  foreign key (account_email) references account (email) on delete cascade on update restrict
);

create sequence project_seq start with 1000;
create sequence task_seq start with 1000;

# --- !Downs

drop table if exists task;
drop table if exists project_account;
drop table if exists project;
drop table if exists account;

drop sequence if exists project_seq;
drop sequence if exists task_seq;


