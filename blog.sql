create database beanan_blog;

use beanan_blog;

create table arcitle_tag_res
(
  article_id varchar(36) not null,
  tag_id     varchar(36) not null,
  primary key (article_id, tag_id)
);

create table article
(
  article_id          varchar(36)  not null
    primary key,
  article_name        varchar(36)  null,
  article_content_pos varchar(100) null,
  create_time         datetime     null,
  update_time         datetime     null,
  read_num            int          null,
  category_id         varchar(36)  null
);

create table blog_test
(
  id varchar(50) not null
    primary key
);

create table category
(
  category_id   varchar(36) not null
    primary key,
  category_name varchar(50) null,
  create_time   datetime    null,
  update_time   datetime    null
);

create table comment
(
  comment_id      varchar(36) not null
    primary key,
  user_name       varchar(36) null,
  user_mail       varchar(36) null,
  comment_content text        null,
  article_id      varchar(36) null,
  create_time     datetime    null
);

create table tag
(
  tag_id      varchar(36) not null
    primary key,
  tag_name    varchar(50) null,
  create_time datetime    null,
  update_time datetime    null
);
