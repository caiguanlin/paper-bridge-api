alter table paper
  modify column unit_name varchar(255) not null,
  modify column chapter_name varchar(255) not null,
  add column scope_type varchar(32) null after chapter_name,
  add column scope_payload_json text null after scope_type;
