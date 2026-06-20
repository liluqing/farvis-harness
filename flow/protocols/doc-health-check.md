# 文档健康度检查

> **何时加载**：每个 Phase 启动前、断点续接前、变更模式完成后、归档前。

---

## 目标

防止 Agent 基于过期或互相矛盾的文档继续开发。

---

## 检查项

| 检查项 | 怎么查 | 异常信号 | 处理 |
|--------|--------|----------|------|
| PRD 时效性 | PRD 修改时间 vs 最近代码/迭代变更 | PRD 明显落后 | 提示更新 PRD 或标注风险继续 |
| 架构一致性 | `tech-design.md` / `project-map.yaml` / 代码目录 | 模块数量或名称不一致 | 先对齐架构或 project-map |
| ai-context 完整性 | `.harness/ai-context/` 是否仍是模板 | 文件空、占位符多 | 从 PRD / project 文档提取 |
| state 一致性 | `state.json` vs 实际代码/测试/迭代目录 | completed 但产物缺失 | 修正 state 或补齐产物 |
| state 存在性 | `.harness/flow/shared/state.json` 文件是否存在 | 文件不存在 | 🔴 阻断：必须先执行 `cp flow/shared/state.json .harness/flow/shared/state.json`（参考 SKILL.md 初始化流程） |
| 迭代目录完整性 | `Docs/iterations/{迭代名}/` | 缺少标准文件 | 从模板补齐 |
| ADR 有效性 | ADR 中的技术决策是否仍适用 | 决策已变更但未废弃 | 新增或废弃 ADR |

---

## 检查时机

| 时机 | 检查重点 |
|------|----------|
| Phase 1 启动 | ai-context 是否为空、是否已有活跃迭代 |
| Phase 2 启动 | PRD 是否确认、架构是否过时 |
| Phase 3 启动 | PRD + 架构 + ai-context + state 一致性 |
| Phase 4 启动 | Phase 3 是否 completed，切片是否全部通过 |
| 断点续接 | state 与实际产物是否一致 |
| 变更完成 | 文档是否随代码同步更新 |
| 归档前 | `_meta.yaml`、tasks、DDL/API/review-notes 是否完整 |

---

## 结果处理

```
全部通过
  → 进入目标 Phase

只有警告
  → 输出警告列表
  → 让用户选择：先更新文档 / 知情继续 / 让 Agent 对比分析

严重不一致
  → 阻断
  → 先修复文档或 state
```

严重不一致包括：

- PRD 不存在但要进 Phase 3
- Phase 4 启动时仍有 failed 切片
- 迭代目录缺失
- `project-map.yaml` 与实际模块结构完全不匹配
- 归档目标迭代未 completed

---

## 文档同步铁律

改完代码必须同步文档，改完文档必须同步代码。不能只改一边。

每次变更或迭代完成后，Agent 自检：

1. 需求变了吗？如果变了，更新 `prd.md`
2. 模块/接口/数据模型变了吗？如果变了，更新 `tech-design.md`、`ddl-changes.md`、`api-changes.md`
3. 新增错误码了吗？如果有，更新 `error-catalog.yaml`
4. 新增业务规则了吗？如果有，更新 `business-rules.yaml`
5. 模块依赖变了吗？如果有，更新 `project-map.yaml`
6. 做了技术决策吗？如果有，新增或更新 ADR

---

## 输出模板

```markdown
【文档健康度检查】

结果：通过 / 警告 / 阻断

检查项：
- PRD：✅/⚠️/❌ <说明>
- 架构：✅/⚠️/❌ <说明>
- ai-context：✅/⚠️/❌ <说明>
- state.json：✅/⚠️/❌ <说明>
- 迭代目录：✅/⚠️/❌ <说明>

下一步：
<继续进入 Phase / 先修复某文档 / 等用户确认>
```
