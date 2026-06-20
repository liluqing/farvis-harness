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
  archive_date: "2026-06-19"
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


