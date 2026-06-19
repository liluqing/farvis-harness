# 系统架构

> **最后更新**：2026-06-19（项目初始化）
> **维护规则**：仅在迭代归档时更新，迭代进行中保持不变

---

## 1. 架构全景

Farvis-AI 是面向企业营销和教育培训的 AI 数字人视频生成平台。采用经典的前后端分离架构，后端 Spring Boot 3.x 提供 RESTful API，前端 React 构建工作台 UI，核心视频生成能力依赖 HeyGen API v3。

---

## 2. 技术选型

| 层次 | 技术 | 版本 | 选型理由 |
|------|------|------|---------|
| 后端框架 | Spring Boot | 3.x | 成熟稳定，生态完善 |
| 前端框架 | React | 18+ | 组件化开发，工作台 UI 复杂度高 |
| 数据库 | MySQL | 8.0+ | 关系型数据，事务支持 |
| 缓存 | Redis | 7+ | 积分余额缓存、限流计数 |
| 对象存储 | OSS | - | 视频/图片文件存储 |

---

## 3. 模块关系

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ farvis-video │────▶│ farvis-heygen│────▶│  HeyGen API  │
└──────────────┘     └──────────────┘     └──────────────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐
│ farvis-avatar│     │ farvis-voice │
└──────────────┘     └──────────────┘
       │                    │
       └────────┬───────────┘
                ▼
        ┌──────────────┐     ┌──────────────┐
        │farvis-credits│────▶│farvis-payment│
        └──────────────┘     └──────────────┘
```

| 模块 | 职责 | 依赖 | 被依赖 |
|------|------|------|--------|
| farvis-video | 视频生成核心流程 | farvis-heygen, farvis-credits | 无 |
| farvis-avatar | 数字人形象管理 | farvis-heygen | farvis-video |
| farvis-voice | 语音合成管理 | farvis-heygen | farvis-video |
| farvis-credits | 积分体系（充值/消耗/流水） | 无 | farvis-video, farvis-avatar, farvis-voice |
| farvis-payment | 支付模块（套餐购买） | farvis-credits | 无 |
| farvis-heygen | HeyGen SDK 封装层 | 外部 HeyGen API | farvis-video, farvis-avatar, farvis-voice |

---

## 4. 部署拓扑

```
[浏览器] → [Nginx] → [React SPA]
                  └→ [Spring Boot API] → [MySQL + Redis]
                                      └→ [HeyGen API（需代理）]
                                      └→ [OSS]
```

---

## 5. 核心约束

| 约束 | 说明 |
|------|------|
| 单租户 | v1.0 不支持多租户，所有用户共享一套资源 |
| HeyGen API 代理 | 国内网络无法直连 HeyGen，需通过代理访问 |
| HeyGen 配额 | 1000 次/月，需做限流和降级处理 |
| 视频输出 | 1080P 高清，由 HeyGen API 保证 |

---

## 6. 变更历史

| 迭代 | 日期 | 变更内容 |
|------|------|---------|
| 项目初始化 | 2026-06-19 | 初始架构定义 |
