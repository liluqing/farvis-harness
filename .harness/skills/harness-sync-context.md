# harness-sync-context

> **Skill 名称**：harness-sync-context
> **用途**：同步 AI Context 文档，从 Docs/ 目录中提取摘要和索引
> **触发时机**：迭代归档后 / 手动触发 / 定时任务

---

## 使用场景

当以下情况发生时，调用此 Skill：
1. 迭代归档完成，需要更新 AI Context
2. 用户手动要求"刷新 AI Context"
3. 定时任务触发（如每天一次）

---

## 执行流程

### 1. 读取同步元数据

```bash
cat Docs/.ai-context-sync.json
```

获取：
- `last_sync`: 上次同步时间
- `file_snapshots`: 上次同步时的文件快照（hash + mtime）

### 2. 扫描变更文件

扫描以下目录的文件：
- `Docs/project/` — 项目当前状态
- `Docs/archive/` — 已归档迭代
- `Docs/iterations/` — 活跃迭代

计算每个文件的：
- `hash`: 文件内容的 MD5 或 SHA256
- `mtime`: 文件最后修改时间

对比 `file_snapshots`，识别变更文件。

### 3. 分类变更类型

根据变更文件的路径，分类为：

| 变更类型 | 文件路径模式 | 影响的 AI Context 区块 |
|---------|-------------|----------------------|
| 项目状态变更 | `Docs/project/*.md` | 当前状态摘要、模块索引 |
| 新迭代出现 | `Docs/iterations/*/` | 进行中迭代 |
| 迭代归档 | `Docs/archive/*/` | 迭代历史摘要 |
| 模块变更 | `Docs/project/modules/*.md` | 模块索引 |

### 4. 更新 AI Context

根据变更类型，更新 `Docs/AI-CONTEXT.md` 的对应区块：

#### 4.1 项目状态变更

如果 `Docs/project/data-model.md`、`Docs/project/api-contracts.md` 或 `Docs/project/architecture.md` 有变更：
- 重新读取这些文件
- 提取关键信息（如新增的表、接口、模块）
- 更新"当前状态摘要"区块

#### 4.2 新迭代出现

如果 `Docs/iterations/` 下有新目录：
- 读取新迭代的 `_meta.yaml`
- 在"进行中迭代"表格中追加一行

#### 4.3 迭代归档

如果 `Docs/archive/` 下有新目录：
- 读取归档迭代的 `_meta.yaml`
- 在"迭代历史摘要"表格中追加一行
- 更新"当前状态摘要"中的模块状态（如有模块从"开发中"变为"已上线"）

#### 4.4 模块变更

如果 `Docs/project/modules/*.md` 有变更：
- 读取变更的模块文档
- 更新"模块索引"表格中的"最后更新"日期

### 5. 更新同步元数据

更新 `Docs/.ai-context-sync.json`：
- `last_sync`: 当前时间
- `file_snapshots`: 更新变更文件的 hash 和 mtime
- `last_sync_changes`: 记录本次同步的变更列表

### 6. Git 提交（可选）

如果 AI Context 有变更：
```bash
git add Docs/AI-CONTEXT.md Docs/.ai-context-sync.json
git commit -m "docs: sync AI Context"
```

---

## 输出格式

执行完成后，输出摘要：

```
✅ AI Context 同步完成
- 最后同步时间：{timestamp}
- 本次变更：
  - {变更 1}
  - {变更 2}
- 详情见：Docs/.ai-context-sync.json
```

---

## 异常处理

### 异常 1：`.ai-context-sync.json` 不存在或损坏

**处理方式**：
1. 删除 `.ai-context-sync.json`（如存在）
2. 执行全量同步：扫描所有文件，重建完整的 `file_snapshots`
3. 重新生成 AI Context

### 异常 2：AI Context 模板区块缺失

**处理方式**：
1. 从 `core-design/templates/ai-context.md` 复制缺失的区块
2. 填充当前信息

---

## 示例调用

```bash
# 在 Agent 中调用
请执行 harness-sync-context Skill，刷新 AI Context
```

---

## 验证标准

- [ ] `Docs/AI-CONTEXT.md` 的"最后同步时间"已更新
- [ ] `Docs/.ai-context-sync.json` 的 `last_sync` 已更新
- [ ] 变更文件已记录到 `last_sync_changes`
- [ ] AI Context 的内容与 Docs/ 目录一致
