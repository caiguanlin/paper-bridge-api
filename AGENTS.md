# Paper Bridge API

小学试卷生成系统后端 API，服务小学老师快速生成、编辑和导出试卷。

## Tech Stack

- **Java 21 + Spring Boot 3** - 后端框架
- **MySQL + JPA** - 数据持久化
- **JWT** - 认证
- **Deepseek API** - AI 补题（Mock 模式可用）

## Project Structure

```
src/main/java/com/paper/teacher/
├── auth/              # 认证：登录、注册、JWT
├── curriculum/        # 教材目录：年级、出版社、科目、册别、单元、章节
├── question/         # 题库：题目 CRUD、Excel 导入
├── paper/             # 试卷：生成、编辑、导出
├── template/          # 题型模板：预定义题型结构
├── ai/                # AI 补题：Deepseek API 调用与校验
└── common/            # 通用：异常处理、响应封装、获取当前用户
```

## Key Concepts

### 试卷快照隔离原则

编辑试卷时**只修改快照，不污染原题库**。题目修改后保存到 `PaperQuestion`（试卷题目快照），原始 `Question`（题库题目）保持不变。

### AI 补题边界

AI 只负责补足题库缺口，不生成整张试卷。请求包含教材范围、题型、数量、难度和已选题目摘要用于避重。

### 双版本导出

学生版隐藏答案和解析，教师版显示完整答案和解析。导出以试卷快照为准。

## Running Tests

```bash
./mvnw test
```

## Running Locally

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Database

SQL语句统一放到 `db/` 目录：

- `V1__init_schema.sql` - 初始表结构
- `V2__seed_pep_primary_curriculum_2026.sql` - 教材目录数据
- `V3__create_question_type_template.sql` - 题型模板

## API Documentation

接口契约文档位于 `docs/` 目录，必要时可查阅详细说明。

## Development Notes

- 前端为独立项目，此仓库仅包含后端 API
- Mock AI 模式可通过配置切换，用于测试 AI 功能
- Excel 导入使用 Apache POI 处理 .xlsx 文件