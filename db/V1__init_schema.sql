create table if not exists teacher_user (
  id bigint primary key auto_increment,
  username varchar(64) not null unique,
  password_hash varchar(255) not null,
  display_name varchar(64) not null,
  created_at datetime not null,
  updated_at datetime not null
);

create table if not exists curriculum_node (
  id bigint primary key auto_increment,
  grade varchar(32) not null,
  publisher varchar(64) not null,
  subject varchar(32) not null,
  volume varchar(32) not null,
  unit_name varchar(64) not null,
  chapter_name varchar(64) not null,
  sort_order int not null,
  edition_year int null,
  source_url varchar(255) null,
  source_code varchar(64) null,
  index idx_curriculum_scope (publisher, subject, grade, volume, unit_name, chapter_name)
);

create table if not exists question (
  id bigint primary key auto_increment,
  owner_user_id bigint not null,
  grade varchar(32) not null,
  publisher varchar(64) not null,
  subject varchar(32) not null,
  volume varchar(32) not null,
  unit_name varchar(64) not null,
  chapter_name varchar(64) not null,
  question_type varchar(32) not null,
  difficulty varchar(32) not null,
  stem text not null,
  content_json text not null,
  answer_json text not null,
  analysis text null,
  source varchar(32) not null,
  usage_count int not null default 0,
  created_at datetime not null,
  updated_at datetime not null,
  index idx_question_scope (owner_user_id, grade, publisher, subject, volume, unit_name, chapter_name, question_type)
);

create table if not exists paper (
  id bigint primary key auto_increment,
  owner_user_id bigint not null,
  title varchar(128) not null,
  grade varchar(32) not null,
  publisher varchar(64) not null,
  subject varchar(32) not null,
  volume varchar(32) not null,
  unit_name varchar(64) not null,
  chapter_name varchar(64) not null,
  total_score decimal(8,2) not null,
  status varchar(32) not null,
  created_at datetime not null,
  updated_at datetime not null
);

create table if not exists paper_section (
  id bigint primary key auto_increment,
  paper_id bigint not null,
  title varchar(64) not null,
  question_type varchar(32) not null,
  question_count int not null,
  score_per_question decimal(8,2) not null,
  subtotal_score decimal(8,2) not null,
  sort_order int not null
);

create table if not exists paper_question (
  id bigint primary key auto_increment,
  paper_id bigint not null,
  section_id bigint not null,
  source_question_id bigint null,
  source varchar(32) not null,
  stem_snapshot text not null,
  content_snapshot_json text not null,
  answer_snapshot_json text not null,
  analysis_snapshot text null,
  score decimal(8,2) not null,
  sort_order int not null
);

create table if not exists question_type_template (
  id bigint primary key auto_increment,
  name varchar(64) not null,
  total_score decimal(8,2) not null,
  sort_order int not null default 0,
  created_at datetime not null,
  updated_at datetime not null
);

create table if not exists question_type_template_item (
  id bigint primary key auto_increment,
  template_id bigint not null,
  title varchar(64) not null,
  question_type varchar(32) not null,
  question_count int not null,
  score_per_question decimal(8,2) not null,
  sort_order int not null,
  index idx_question_type_template_item_template (template_id)
);
