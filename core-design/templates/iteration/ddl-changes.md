# DDL 变更记录

> **迭代**：{YYYY-MM-DD_需求名称_版本}
> **最后更新**：{YYYY-MM-DD}

---

## 变更清单

### 新增表

| 表名 | 用途 | 核心字段 | 备注 |
|------|------|---------|------|
| {table_name} | {用途} | id, name, created_at, updated_at | |

### 修改表

| 表名 | 变更类型 | 字段 | 类型 | 说明 |
|------|---------|------|------|------|
| {table_name} | 新增字段 | {field_name} | {类型} | {说明} |
| {table_name} | 新增索引 | idx_{field} | - | {说明} |
| {table_name} | 修改字段 | {field_name} | {新类型} | {说明} |

### 废弃表/字段

| 对象 | 类型 | 废弃原因 |
|------|------|---------|
| {table_name.field_name} | 字段 | {原因} |

---

## DDL 脚本

> 归档时这些 DDL 会合并到 `project/data-model.md`

```sql
-- 新增表
CREATE TABLE {table_name} (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- ...
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 修改表
ALTER TABLE {table_name} ADD COLUMN {field_name} {type} COMMENT '{注释}';
```
