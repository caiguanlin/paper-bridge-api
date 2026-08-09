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
├── PaperTeacherApplication.java
├── common/            # 通用：异常处理、响应封装、分页、当前用户
├── config/            # 配置：Spring Security、Deepseek 参数等
├── constant/          # 常量与枚举：题型、难度、试卷状态等
├── controller/        # Web 接口入口：认证、教材、题库、试卷、模板
└── modules/           # 业务模块：DTO、Entity、Repository、Service
    ├── auth/          # 认证：登录、注册、JWT
    ├── curriculum/    # 教材目录：年级、出版社、科目、册别、单元、章节
    ├── question/      # 题库：题目 CRUD、Excel 导入
    ├── paper/         # 试卷：生成、编辑、导出
    ├── template/      # 题型模板：预定义题型结构
    └── ai/            # AI 补题：Deepseek API 调用与校验
```


## Configuration

运行前需通过环境变量提供敏感配置（不要写入仓库中的 `application.yml`，本地可用被 gitignore 的 `application-local.yml`）：

| 变量 | 说明 |
| --- | --- |
| `APP_JWT_SECRET` | JWT 签名密钥，至少 32 字节随机内容，缺失或过短时应用启动失败 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接信息 |
| `DEEPSEEK_API_KEY` | Deepseek API Key |

## Running Tests

```bash
./mvnw test
```

## Running Locally

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## 注意事项
- SQL语句统一放到 `db/` 目录
- 接口契约文档位于 `docs/` 目录，必要时可查阅详细说明
