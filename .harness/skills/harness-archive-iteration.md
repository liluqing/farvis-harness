# harness-archive-iteration

> **Skill 名称**：harness-archive-iteration
> **用途**：执行迭代归档流程，将迭代文档合并到项目状态文档
> **触发时机**：用户确认交付完成后说「归档」，或 Phase 4 集成测试通过后
> **权威 Checklist**：`core-design/templates/archive-checklist.md`（v2.0）

---

## 执行入口

**此 Skill 是 `archive-checklist.md` v2.0 的执行实现。** 归档时加载该 checklist，按 5 个阶段依次执行。

```
阶段 1：前置检查（_meta.yaml 状态 + tasks.md 全完成 + git 干净）
    ↓
阶段 2：文档合并（DDL → data-model.md / API → api-contracts.md / 业务规则 → modules/）
    ↓
阶段 3：目录迁移（_meta.yaml → archived，mv iterations/ → archive/）
    ↓
阶段 4：AI Context 同步（AI-CONTEXT.md + .ai-context-sync.json）
    ↓
阶段 5：提交 + 推送
```

---

## 使用场景

当以下情况发生时，调用此 Skill：
1. Phase 4 完成，用户说「归档」
2. 用户手动要求"归档迭代 XXX"

---

## 前置条件

在执行归档前，必须确认：

- [ ] 迭代分支已合并到 main
- [ ] `Docs/iterations/{迭代名}/` 目录存在
- [ ] `_meta.yaml` 中 `status: completed`（不是 `in_progress`）
- [ ] `tasks.md` 中所有任务状态为 `✅ 完成` 或 `❌ 取消`

如果前置条件不满足，提示用户：
```
⚠️ 归档前置条件不满足：
- {不满足的条件 1}
- {不满足的条件 2}

请先解决以上问题，再执行归档。
```

---

## 执行流程

### 1. 读取迭代信息

```bash
cat Docs/iterations/{迭代名}/_meta.yaml
```

获取：
- `iteration`: 迭代名称
- `version`: 版本号
- `modules_affected`: 影响的模块列表
- `summary`: 迭代摘要

### 2. 合并 DDL 变更

读取 `Docs/iterations/{迭代名}/ddl-changes.md`，合并到 `Docs/project/data-model.md`：

#### 2.1 新增表

如果 `ddl-changes.md` 中有"新增表"：
- 在 `data-model.md` 的"表结构总览"中追加新表
- 按模块分组（根据 `_meta.yaml` 的 `modules_affected`）

#### 2.2 修改表

如果 `ddl-changes.md` 中有"修改表"：
- 在 `data-model.md` 中找到对应表
- 更新字段信息

#### 2.3 废弃字段

如果 `ddl-changes.md` 中有"废弃表/字段"：
- 在 `data-model.md` 中标记为 `~~deprecated~~`
- 记录废弃日期

#### 2.4 追加变更历史

在 `data-model.md` 的"变更历史"表格中追加一行：
```
| {迭代名} | {归档日期} | {变更类型} | {对象} | {说明} |
```

### 3. 合并 API 变更

读取 `Docs/iterations/{迭代名}/api-changes.md`，合并到 `Docs/project/api-contracts.md`：

#### 3.1 新增接口

在 `api-contracts.md` 的"接口列表"中追加新接口：
- 按模块分组
- 状态标记为 `✅ 有效`
- 记录引入迭代

#### 3.2 修改接口

在 `api-contracts.md` 中找到对应接口，更新信息。

#### 3.3 废弃接口

在 `api-contracts.md` 中标记为 `~~deprecated~~`，记录废弃日期。

#### 3.4 追加变更历史

在 `api-contracts.md` 的"变更历史"表格中追加一行。

### 4. 合并业务文档

对 `modules_affected` 中的每个模块，读取迭代的 `prd.md` 和 `tech-design.md`，合并到 `Docs/project/modules/{module}.md`：

#### 4.1 更新核心业务规则

- 从 `prd.md` 的"业务规则"区块提取新规则
- 追加或更新到模块文档的"核心业务规则"表格

#### 4.2 更新核心流程

- 从 `tech-design.md` 的"核心流程"区块提取时序图
- 追加或更新到模块文档的"核心流程"区块

#### 4.3 更新边界条件

- 从 `prd.md` 或 `tech-design.md` 中提取边界场景
- 追加到模块文档的"边界条件"表格

#### 4.4 追加变更历史

在模块文档的"变更历史"表格中追加一行。

### 5. 更新架构文档（如有架构变更）

读取 `tech-design.md` 的"架构设计"区块，判断是否有架构级变更：

- 新增模块
- 模块关系变更
- 技术选型变更
- 部署拓扑变更

如有，更新 `Docs/project/architecture.md` 的对应区块。

### 6. 更新迭代元信息

编辑 `Docs/iterations/{迭代名}/_meta.yaml`：
- `status`: 改为 `archived`
- `archive_info.archive_date`: 当天日期
- `archive_info.actual_duration_days`: 计算实际耗时（`archive_date - start_date`）
- `archive_info.retrospective`: 从 `review-notes.md` 的"复盘记录"中提取

### 7. 移动迭代目录

```bash
mv Docs/iterations/{迭代名} Docs/archive/{迭代名}
```

### 8. 调用 AI Context 同步 Skill

执行 `harness-sync-context` Skill，更新 AI Context。

### 9. Git 提交

```bash
git add Docs/
git commit -m "docs: 归档迭代 {迭代名}"
```

---

## 输出格式

执行完成后，输出摘要：

```
✅ 迭代归档完成：{迭代名}

已合并文档：
- DDL 变更 → Docs/project/data-model.md
- API 变更 → Docs/project/api-contracts.md
- 业务文档 → Docs/project/modules/{module1}.md
- 业务文档 → Docs/project/modules/{module2}.md
{如有架构变更}- 架构变更 → Docs/project/architecture.md

已移动目录：
- Docs/iterations/{迭代名}/ → Docs/archive/{迭代名}/

已更新 AI Context：
- 新增归档记录：{迭代名}
- 更新模块状态：{module1}、{module2}

Git 提交：docs: 归档迭代 {迭代名}
```

---

## 异常处理

### 异常 1：文档合并冲突

**场景**：`project/data-model.md` 中的某表已被其他迭代修改。

**处理方式**：
1. 检查两个迭代的变更是否兼容
2. 如兼容：手动合并两个变更
3. 如不兼容：回退到冲突前的状态，提示用户与相关人讨论后再合并

### 异常 2：归档后发现遗漏

**场景**：归档完成后发现某个文档遗漏了重要信息。

**处理方式**：
1. 不要直接修改 `archive/{迭代名}/` 下的文件（已归档只读）
2. 提示用户在新迭代的文档中补充遗漏信息
3. 在 `AI-CONTEXT.md` 的"技术债与待优化项"中记录此遗漏

---

## 示例调用

```bash
# 在 Agent 中调用
请执行 harness-archive-iteration Skill，归档迭代 2026-06-19_用户认证_v1.0
```

---

## 验证标准

- [ ] `Docs/archive/{迭代名}/` 目录存在且文件完整
- [ ] `Docs/iterations/{迭代名}/` 已不存在
- [ ] `Docs/project/data-model.md` 已合并 DDL 变更
- [ ] `Docs/project/api-contracts.md` 已合并 API 变更
- [ ] `Docs/project/modules/*.md` 已合并业务文档
- [ ] `Docs/AI-CONTEXT.md` 已更新（通过 `harness-sync-context` Skill）
- [ ] Git 提交成功
