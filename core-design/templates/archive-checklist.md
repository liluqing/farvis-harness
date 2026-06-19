# 归档流程 Checklist（v2.0 轻量版）

> **使用场景**：迭代开发完成后，执行归档操作时对照此 checklist
> **执行者**：Agent（自动执行）+ 人（审阅确认）
> **设计原则**：每个阶段都有明确的"通过标准"，避免过度检查

---

## 阶段 1：前置检查（3 项核心检查）

### 通过标准
- [ ] `_meta.yaml` 中 `status: completed`
- [ ] `tasks.md` 中所有任务状态为 `✅ 完成` 或 `❌ 取消`
- [ ] `git status` 干净（无未提交的变更）

### 快速检查命令
```bash
# 检查 _meta.yaml
grep "status:" Docs/iterations/{迭代名}/_meta.yaml

# 检查 tasks.md
grep "🔄\|⏸️" Docs/iterations/{迭代名}/tasks.md && echo "有未完成的任务" || echo "全部完成"

# 检查 git 状态
git status --porcelain | wc -l
```

**如果以上 3 项全部通过 → 进入阶段 2**  
**如果有任一项不通过 → 停止，先修复**

---

## 阶段 2：文档合并（3 个文件，按模块追加）

### 合并原则
- **追加模式**：新增内容追加到 `project/` 对应文件的末尾
- **模块对齐**：按 `_meta.yaml` 中的 `modules_affected` 字段确定影响范围
- **不重复**：如果 `project/` 中已有相同内容，跳过

### 2.1 DDL 变更合并

**源文件**：`iterations/{迭代名}/ddl-changes.md`  
**目标文件**：`project/data-model.md`

**操作步骤**：
1. 读取 `ddl-changes.md`，提取所有 `CREATE TABLE` 和 `ALTER TABLE`
2. 在 `data-model.md` 末尾追加：
   ```markdown
   ## 迭代 {迭代名} 新增表结构
   
   ### {表名}
   （粘贴 CREATE TABLE 语句）
   
   **索引**：（列出关键索引）
   **约束**：（列出 CHECK 约束）
   ```
3. 在 `data-model.md` 的"变更历史"表格中追加一行

### 2.2 API 变更合并

**源文件**：`iterations/{迭代名}/api-changes.md`  
**目标文件**：`project/api-contracts.md`

**操作步骤**：
1. 读取 `api-changes.md`，提取所有 `A1`、`A2` 等接口定义
2. 在 `api-contracts.md` 的对应模块下追加：
   ```markdown
   ## 迭代 {迭代名} 新增接口
   
   ### A1: GET /api/v1/credits/balance
   （粘贴完整接口定义，包括请求/响应示例）
   ```
3. 在 `api-contracts.md` 的"变更历史"表格中追加一行

### 2.3 业务规则合并

**源文件**：`iterations/{迭代名}/tech-design.md`（业务规则部分）  
**目标文件**：`project/modules/{module}.md`（每个受影响的模块）

**操作步骤**：
1. 读取 `_meta.yaml` 的 `modules_affected` 字段，确定影响哪些模块
2. 对每个模块：
   - 打开 `project/modules/{module}.md`
   - 在"业务规则"部分追加新规则
   - 在"变更历史"表格中追加一行

### 通过标准
- [ ] `data-model.md` 已追加所有新表
- [ ] `api-contracts.md` 已追加所有新接口
- [ ] 每个受影响模块的 `modules/{module}.md` 已追加业务规则
- [ ] 所有"变更历史"表格已追加记录

---

## 阶段 3：目录迁移（2 步操作）

### 3.1 更新 `_meta.yaml`

```yaml
status: archived
archive_info:
  archived_at: "2026-06-19T22:30:00+08:00"
  archived_by: "AI Agent"
  notes: "归档完成"
```

### 3.2 移动目录

```bash
mv Docs/iterations/{迭代名} Docs/archive/{迭代名}
```

### 通过标准
- [ ] `_meta.yaml` 中 `status: archived`
- [ ] `Docs/archive/{迭代名}/` 存在
- [ ] `Docs/iterations/{迭代名}/` 不存在

---

## 阶段 4：AI Context 同步（1 个文件，3 个区块）

**目标文件**：`Docs/AI-CONTEXT.md`

### 操作步骤

1. **更新"当前迭代"区块**：
   ```markdown
   ## 当前迭代
   （无）
   ```

2. **更新"模块状态摘要"区块**：
   - 找到 `modules_affected` 中列出的模块
   - 将状态从"开发中"改为"已上线"或"已完成"

3. **追加"迭代历史"区块**：
   ```markdown
   ### {迭代名}（已归档）
   - **归档时间**：2026-06-19
   - **影响模块**：farvis-credits, farvis-payment
   - **核心变更**：新增 3 张表、6 个接口、积分体系完整实现
   ```

### 通过标准
- [ ] `AI-CONTEXT.md` 中"当前迭代"为空
- [ ] 受影响模块的状态已更新
- [ ] "迭代历史"已追加本次归档记录

---

## 阶段 5：提交 + 推送（1 个 commit）

```bash
git add Docs/
git commit -m "docs: 归档迭代 {迭代名}"
git push origin main
```

### 通过标准
- [ ] commit 成功
- [ ] push 成功

---

## 归档完成 ✅

**输出给用户的消息**：
```
✅ 迭代 {迭代名} 已归档完成。

归档摘要：
- 影响模块：{modules_affected}
- 新增表：X 张
- 新增接口：Y 个
- 归档位置：Docs/archive/{迭代名}/

下一步：
- 可以开始新的迭代（说"开始迭代 XXX"）
- 或者查看项目状态（说"项目状态"）
```

---

## 阶段 2：文档合并

> 将迭代中的变更记录合并到 `project/` 目录

### 2.1 DDL 变更合并

- [ ] 读取 `iterations/{迭代名}/ddl-changes.md`
- [ ] 将新增表追加到 `project/data-model.md` 的对应模块
- [ ] 将修改字段更新到 `project/data-model.md` 的对应表
- [ ] 将废弃字段标记为 `~~deprecated~~` 并记录废弃日期
- [ ] 在 `project/data-model.md` 的"变更历史"区块追加一条记录
- [ ] **验证**：`project/data-model.md` 中的表结构与当前数据库一致

### 2.2 API 变更合并

- [ ] 读取 `iterations/{迭代名}/api-changes.md`
- [ ] 将新增接口追加到 `project/api-contracts.md` 的对应模块
- [ ] 将修改接口更新到 `project/api-contracts.md` 的对应接口
- [ ] 将废弃接口标记为 `~~deprecated~~` 并记录废弃日期
- [ ] 在 `project/api-contracts.md` 的"变更历史"区块追加一条记录
- [ ] **验证**：`project/api-contracts.md` 中的接口列表与当前代码一致

### 2.3 业务文档合并

- [ ] 读取 `iterations/{迭代名}/prd.md` 和 `tech-design.md`
- [ ] 确定本次迭代影响的模块（从 `_meta.yaml` 的 `modules_affected` 获取）
- [ ] 对每个受影响模块：
  - [ ] 打开 `project/modules/{module}.md`
  - [ ] 更新"核心业务规则"（新增/修改/删除规则）
  - [ ] 更新"核心流程"（如有流程变更）
  - [ ] 更新"边界条件"（如有新增边界场景）
  - [ ] 在"变更历史"区块追加一条记录
- [ ] **验证**：`project/modules/*.md` 中的业务规则与当前代码逻辑一致

### 2.4 架构文档合并（如有架构变更）

- [ ] 判断本次迭代是否有架构级变更（从 `tech-design.md` 的"架构设计"区块判断）
- [ ] 如有：
  - [ ] 更新 `project/architecture.md` 的"模块关系"
  - [ ] 更新 `project/architecture.md` 的"技术选型"（如有新技术引入）
  - [ ] 更新 `project/architecture.md` 的"部署拓扑"（如有部署变更）
  - [ ] 在"变更历史"区块追加一条记录
- [ ] 如无：跳过此步骤

---

## 阶段 3：目录迁移

### 3.1 更新迭代元信息

- [ ] 打开 `iterations/{迭代名}/_meta.yaml`
- [ ] 将 `status` 改为 `archived`
- [ ] 填写 `archive_info.archive_date`（当天日期）
- [ ] 填写 `archive_info.actual_duration_days`（实际耗时天数）
- [ ] 填写 `archive_info.retrospective`（一句话复盘）

### 3.2 移动目录

- [ ] 执行 `mv iterations/{迭代名} archive/{迭代名}`
- [ ] **验证**：`archive/{迭代名}/` 存在且文件完整
- [ ] **验证**：`iterations/{迭代名}/` 已不存在

---

## 阶段 4：AI Context 同步

### 4.1 更新 AI Context

- [ ] 打开 `Docs/AI-CONTEXT.md`
- [ ] 更新"当前状态摘要"区块：
  - [ ] 更新受影响模块的状态（如从"开发中"改为"已上线"）
  - [ ] 删除"进行中迭代"中的本迭代记录
- [ ] 更新"模块索引"区块：
  - [ ] 更新受影响模块的"状态"和"最后更新"日期
- [ ] 更新"迭代历史摘要"区块：
  - [ ] 追加一条本迭代的归档记录（迭代名 + 归档位置 + 核心变更摘要）
- [ ] 更新"同步信息"区块：
  - [ ] 更新"最后同步时间"
  - [ ] 更新"同步触发源"为"归档完成（{迭代名}）"
  - [ ] 更新"本次同步变更"列表

### 4.2 更新同步元数据

- [ ] 打开 `Docs/.ai-context-sync.json`
- [ ] 更新 `last_sync` 为当前时间
- [ ] 更新 `file_snapshots` 中变更文件的 hash 和 mtime
- [ ] 更新 `last_sync_changes` 为本次同步的变更列表

---

## 阶段 5：最终验证

### 5.1 文档一致性检查

- [ ] **检查 1**：`project/data-model.md` 中的表是否与数据库实际表结构一致
- [ ] **检查 2**：`project/api-contracts.md` 中的接口是否与代码中的 Controller 一致
- [ ] **检查 3**：`project/modules/*.md` 中的业务规则是否与代码逻辑一致
- [ ] **检查 4**：`AI-CONTEXT.md` 中的模块状态是否与 `project/modules/*.md` 一致

### 5.2 Git 提交

- [ ] 执行 `git add Docs/`
- [ ] 执行 `git commit -m "docs: 归档迭代 {迭代名}"`
- [ ] 执行 `git push origin main`

---

## 阶段 6：通知与记录（可选）

- [ ] （可选）通知相关人员归档完成
- [ ] （可选）在项目周报/月报中记录本次迭代归档

---

## 异常处理

### 异常 1：文档合并冲突

**场景**：`project/data-model.md` 中的某表已被其他迭代修改，本次合并时产生冲突。

**处理方式**：
1. 先检查两个迭代的变更是否兼容
2. 如兼容：手动合并两个变更
3. 如不兼容：回退到冲突前的状态，与相关人讨论后再合并

### 异常 2：归档后发现遗漏

**场景**：归档完成后发现某个文档遗漏了重要信息。

**处理方式**：
1. 不要直接修改 `archive/{迭代名}/` 下的文件（已归档只读）
2. 在新迭代的文档中补充遗漏信息
3. 在 `AI-CONTEXT.md` 的"技术债与待优化项"中记录此遗漏

### 异常 3：AI Context 同步失败

**场景**：`.ai-context-sync.json` 损坏或丢失。

**处理方式**：
1. 删除 `.ai-context-sync.json`
2. 重新执行全量同步（扫描所有 `project/` 和 `archive/` 文档，重建 AI Context）

---

## 执行示例

```bash
# 假设迭代名：2026-06-19_用户认证_v1.0

# 阶段 3.2：移动目录
mv Docs/iterations/2026-06-19_用户认证_v1.0 Docs/archive/2026-06-19_用户认证_v1.0

# 阶段 5.2：Git 提交
cd Docs
git add .
git commit -m "docs: 归档迭代 2026-06-19_用户认证_v1.0"
git push origin main
```

---

## Checklist 总结

| 阶段 | 核心动作 | 验证标准 |
|------|---------|---------|
| 1. 前置检查 | 确认迭代状态、文档完整性、Git 状态 | 所有检查项通过 |
| 2. 文档合并 | DDL/API/业务/架构文档合并到 `project/` | 合并后的文档与实际一致 |
| 3. 目录迁移 | 更新 `_meta.yaml`，移动目录到 `archive/` | 目录已移动，`iterations/` 中无此迭代 |
| 4. AI Context 同步 | 更新 `AI-CONTEXT.md` 和 `.ai-context-sync.json` | AI Context 反映最新状态 |
| 5. 最终验证 | 文档一致性检查 + Git 提交 | 所有检查通过，已推送到远程 |
| 6. 通知记录 | （可选）通知相关人员 | - |
