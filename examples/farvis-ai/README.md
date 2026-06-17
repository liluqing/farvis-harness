# 示例：Farvis-AI

Farvis-AI 是一个数字人视频生成平台（Spring Boot 3.x + MySQL + Redis + HeyGen API）。

本目录展示：用 Harness 模板**实际填写**后的样子，用于验证 ai-context/ 模板结构是否合理。

## 已填写

- `ai-context/project-map.yaml` — 6 个模块的边界和依赖
- `ai-context/business-rules.yaml` — 视频生成、数字人创建、支付等业务约束
- `ai-context/error-catalog.yaml` — 10 个常见错误的修复路径

## 未填写（需求不涉及 / 尚未沉淀）

- `ai-context/coding-rules.yaml` — 直接复用 core/ 中的通用规则

## 注意

- 本目录是**填写示例**，不包含可运行的代码
- 实际使用时，删除本目录或替换为自己的项目信息
