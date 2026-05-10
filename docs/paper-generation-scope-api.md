# 组卷范围接口文档

本文档说明本次 `PaperController#generate` 范围模型重构后，前端需要对接的接口契约。

## 通用说明

接口统一返回：

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

失败时：

```json
{
  "success": false,
  "data": null,
  "message": "错误信息"
}
```

需要登录态的接口请携带：

```http
Authorization: Bearer <token>
Content-Type: application/json
```

## 影响接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/papers/preview-plan` | `POST` | 按组卷范围预览题库数量和 AI 补题数量 |
| `/api/papers/generate` | `POST` | 按组卷范围生成试卷 |
| `/api/papers/{paperId}/regenerate` | `POST` | 重新按原试卷保存的组卷范围生成 |

`preview-plan` 和 `generate` 使用同一个请求体。`regenerate` 不需要请求体，但会读取原试卷保存的 `scopeType` 和范围快照。

## 请求体

```json
{
  "title": "三年级上册期中测试",
  "grade": "三年级",
  "publisher": "PEP",
  "subject": "MATH",
  "volume": "上册",
  "scopeType": "UNITS",
  "units": ["第一单元", "第二单元"],
  "chapters": null,
  "totalScore": 100,
  "strategy": "BANK_WITH_AI",
  "difficulty": "MEDIUM",
  "sections": [
    {
      "title": "一、判断题",
      "questionType": "TRUE_FALSE",
      "questionCount": 10,
      "scorePerQuestion": 2
    }
  ]
}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `title` | string | 是 | 试卷标题 |
| `grade` | string | 是 | 年级 |
| `publisher` | string | 是 | 出版社 |
| `subject` | string | 是 | 科目 |
| `volume` | string | 是 | 册别 |
| `scopeType` | string | 是 | 组卷范围类型：`CHAPTERS`、`UNITS`、`VOLUME` |
| `units` | string[] | 条件必填 | `scopeType=UNITS` 时必填，至少 1 个 |
| `chapters` | object[] | 条件必填 | `scopeType=CHAPTERS` 时必填，至少 1 个 |
| `totalScore` | number | 是 | 总分，必须等于各题型区块小计之和 |
| `strategy` | string | 是 | 生成策略 |
| `difficulty` | string | 否 | 难度，不传则 AI 补题默认按 `MEDIUM` |
| `sections` | object[] | 是 | 题型区块，至少 1 个 |

### 枚举值

`scopeType`：

| 值 | 说明 |
| --- | --- |
| `CHAPTERS` | 精确章节范围，可跨单元选择章节 |
| `UNITS` | 多单元范围，包含所选单元下全部章节 |
| `VOLUME` | 整册范围，不限制单元和章节 |

`strategy`：

| 值 | 说明 |
| --- | --- |
| `BANK_ONLY` | 只从题库抽题，题库不足时报错 |
| `BANK_WITH_AI` | 题库不足时用 AI 补足 |
| `AI_ONLY` | 全部使用 AI 生成 |
| `BANK_FIRST` | 优先题库，缺口用 AI 补足 |

`difficulty`：

```text
EASY, MEDIUM, HARD
```

`questionType`：

```text
SINGLE_CHOICE, TRUE_FALSE, FILL_BLANK, MATCHING, DICTATION
```

## 三种范围请求示例

### 1. 精确章节：CHAPTERS

适合章节小测，也支持跨单元精确选择章节。

```json
{
  "title": "三年级上册章节小测",
  "grade": "三年级",
  "publisher": "PEP",
  "subject": "MATH",
  "volume": "上册",
  "scopeType": "CHAPTERS",
  "chapters": [
    {
      "unit": "第一单元",
      "chapter": "时、分、秒"
    },
    {
      "unit": "第三单元",
      "chapter": "毫米、分米的认识"
    }
  ],
  "totalScore": 20,
  "strategy": "BANK_WITH_AI",
  "difficulty": "MEDIUM",
  "sections": [
    {
      "title": "一、判断题",
      "questionType": "TRUE_FALSE",
      "questionCount": 10,
      "scorePerQuestion": 2
    }
  ]
}
```

校验规则：

- `chapters` 必须至少 1 项。
- 每项的 `unit` 和 `chapter` 都不能为空。
- `units` 可不传。

### 2. 多单元：UNITS

适合期中考试或多个单元复习。

```json
{
  "title": "三年级上册期中测试",
  "grade": "三年级",
  "publisher": "PEP",
  "subject": "MATH",
  "volume": "上册",
  "scopeType": "UNITS",
  "units": ["第一单元", "第二单元", "第三单元"],
  "totalScore": 100,
  "strategy": "BANK_WITH_AI",
  "difficulty": "MEDIUM",
  "sections": [
    {
      "title": "一、选择题",
      "questionType": "SINGLE_CHOICE",
      "questionCount": 20,
      "scorePerQuestion": 3
    },
    {
      "title": "二、判断题",
      "questionType": "TRUE_FALSE",
      "questionCount": 20,
      "scorePerQuestion": 2
    }
  ]
}
```

校验规则：

- `units` 必须至少 1 项。
- `units` 每项不能为空。
- `chapters` 可不传。

### 3. 整册：VOLUME

适合期末考试。

```json
{
  "title": "三年级上册期末测试",
  "grade": "三年级",
  "publisher": "PEP",
  "subject": "MATH",
  "volume": "上册",
  "scopeType": "VOLUME",
  "totalScore": 100,
  "strategy": "BANK_WITH_AI",
  "difficulty": "MEDIUM",
  "sections": [
    {
      "title": "一、选择题",
      "questionType": "SINGLE_CHOICE",
      "questionCount": 20,
      "scorePerQuestion": 3
    },
    {
      "title": "二、判断题",
      "questionType": "TRUE_FALSE",
      "questionCount": 20,
      "scorePerQuestion": 2
    }
  ]
}
```

校验规则：

- 不需要传 `units`。
- 不需要传 `chapters`。
- 后端按 `grade + publisher + subject + volume` 查询整册题库。

## POST /api/papers/preview-plan

用于生成前预览每个题型区块的题库可用数量和 AI 补题数量。

### 响应示例

```json
{
  "success": true,
  "data": {
    "totalScore": 100,
    "subtotalScore": 100,
    "sections": [
      {
        "title": "一、选择题",
        "questionType": "SINGLE_CHOICE",
        "requiredCount": 20,
        "availableBankCount": 12,
        "aiSupplementCount": 8,
        "subtotalScore": 60
      },
      {
        "title": "二、判断题",
        "questionType": "TRUE_FALSE",
        "requiredCount": 20,
        "availableBankCount": 20,
        "aiSupplementCount": 0,
        "subtotalScore": 40
      }
    ]
  },
  "message": null
}
```

说明：

- `requiredCount`：该题型需要的题目数量。
- `availableBankCount`：当前范围内题库可用题目数量。
- `aiSupplementCount`：按当前策略需要 AI 补足的数量。
- `BANK_ONLY` 策略下 `aiSupplementCount` 固定为 `0`。

## POST /api/papers/generate

按请求范围生成试卷。

### 响应示例

```json
{
  "success": true,
  "data": {
    "id": 1001,
    "title": "三年级上册期中测试",
    "grade": "三年级",
    "publisher": "PEP",
    "subject": "MATH",
    "volume": "上册",
    "unit": "第一单元, 第二单元, 第三单元",
    "chapter": "全部章节",
    "totalScore": 100,
    "status": "DRAFT",
    "updatedAt": "2026-05-10T16:54:29",
    "sections": [
      {
        "id": 2001,
        "title": "一、选择题",
        "questionType": "SINGLE_CHOICE",
        "questionCount": 20,
        "scorePerQuestion": 3,
        "subtotalScore": 60,
        "sortOrder": 1,
        "questions": [
          {
            "id": 3001,
            "sourceQuestionId": 501,
            "source": "BANK",
            "stemSnapshot": "题干内容",
            "contentSnapshotJson": "{\"options\":[\"A. 1\",\"B. 2\"]}",
            "answerSnapshotJson": "{\"correctOption\":\"A\"}",
            "analysisSnapshot": "解析内容",
            "score": 3,
            "sortOrder": 1
          }
        ]
      }
    ]
  },
  "message": null
}
```

### 响应中的范围展示字段

当前响应仍保留旧字段：

| 字段 | CHAPTERS 示例 | UNITS 示例 | VOLUME 示例 |
| --- | --- | --- | --- |
| `unit` | `第一单元, 第三单元` | `第一单元, 第二单元` | `整册` |
| `chapter` | `第一单元 / 时、分、秒, 第三单元 / 毫米、分米的认识` | `全部章节` | `全部章节` |

注意：响应暂未返回 `scopeType`、`units`、`chapters` 原始结构。前端生成后展示可使用 `unit` 和 `chapter` 文本。

## POST /api/papers/{paperId}/regenerate

重新生成指定试卷。

请求：

```http
POST /api/papers/1001/regenerate
```

无请求体。

行为说明：

- 后端读取原试卷保存的 `scopeType` 和范围快照重新组卷。
- 重新组卷使用 `BANK_FIRST` 策略。
- 原试卷的题型区块、题数、每题分值、总分会沿用。

响应结构同 `/api/papers/generate`。

## 常见错误

### 总分不匹配

当 `totalScore` 不等于所有 `sections` 的 `questionCount * scorePerQuestion` 之和：

```json
{
  "success": false,
  "data": null,
  "message": "总分必须等于各题型小计之和，当前小计为 90"
}
```

### CHAPTERS 未选择章节

```json
{
  "success": false,
  "data": null,
  "message": "CHAPTERS 范围必须至少选择一个章节"
}
```

### UNITS 未选择单元

```json
{
  "success": false,
  "data": null,
  "message": "UNITS 范围必须至少选择一个单元"
}
```

### BANK_ONLY 题库不足

```json
{
  "success": false,
  "data": null,
  "message": "一、判断题题库数量不足，缺少 3 道题"
}
```

## 前端迁移提示

旧请求字段：

```json
{
  "unit": "第三单元",
  "chapters": ["测量", "千米的认识"]
}
```

新请求字段：

```json
{
  "scopeType": "CHAPTERS",
  "chapters": [
    {
      "unit": "第三单元",
      "chapter": "测量"
    },
    {
      "unit": "第三单元",
      "chapter": "千米的认识"
    }
  ]
}
```

前端建议：

- 章节小测使用 `CHAPTERS`。
- 期中或多单元复习使用 `UNITS`。
- 期末使用 `VOLUME`。
- `totalScore` 在提交前先按题型区块本地计算校验一次。
