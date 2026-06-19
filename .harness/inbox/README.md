# Event Inbox

> 此目录用于存放外部事件（Git Hook、CI 等触发的异步事件）
> 
> Agent 启动时扫描此目录，处理事件后移动到 `inbox-processed/`

## 事件文件格式

参见 `core-design/schemas/inbox-events.json`

## 事件类型

- `branch-created`: 从 main 创建新分支
- `branch-merged`: 分支合并到 main
- `commit-pushed`: 推送 commit
- `ci-failed`: CI 失败
- `pr-review-requested`: PR 评审请求
