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

insert into question_type_template (name, total_score, sort_order, created_at, updated_at)
select '100分基础模板', 100.00, 1, now(), now()
where not exists (select 1 from question_type_template where name = '100分基础模板');

insert into question_type_template_item (template_id, title, question_type, question_count, score_per_question, sort_order)
select template.id, '选择题', 'SINGLE_CHOICE', 10, 5.00, 1
from question_type_template template
where template.name = '100分基础模板'
  and not exists (select 1 from question_type_template_item item where item.template_id = template.id);

insert into question_type_template_item (template_id, title, question_type, question_count, score_per_question, sort_order)
select template.id, '判断题', 'TRUE_FALSE', 10, 2.00, 2
from question_type_template template
where template.name = '100分基础模板'
  and not exists (
    select 1 from question_type_template_item item
    where item.template_id = template.id and item.question_type = 'TRUE_FALSE'
  );

insert into question_type_template_item (template_id, title, question_type, question_count, score_per_question, sort_order)
select template.id, '填空题', 'FILL_BLANK', 10, 3.00, 3
from question_type_template template
where template.name = '100分基础模板'
  and not exists (
    select 1 from question_type_template_item item
    where item.template_id = template.id and item.question_type = 'FILL_BLANK'
  );
