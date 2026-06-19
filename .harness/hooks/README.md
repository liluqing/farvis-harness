# AI 编码工具 Hooks

此目录存放各 AI 编码工具的 hook 脚本。

当前状态：Git hooks 已实现（fastTest + docs-sync 检查），其他工具 hook 待确定。
确定后在此放置脚本，初始化时同样通过软链接接入工具目录。

## 目录约定

```
hooks/
├── git/                   # Git hooks（源文件）
│   ├── pre-commit         # 提交前 fastTest + 文档同步检查
│   ├── pre-push           # 推送前全量测试
│   └── commit-msg         # 提交信息格式校验
├── claude/                # Claude Code hooks（占位）
├── codex/                 # Codex hooks（占位）
└── qoder/                 # Qoder hooks（占位）
```

## Git Hooks 详细说明

### pre-commit

执行两项检查：

**Check 1: fastTest**
- Gradle 项目：运行 `./gradlew fastTest`（切片级测试，≤3s）
- Maven 项目：运行 `mvn test -pl '!integration-test'`
- 任一失败 → 阻止提交

**Check 2: docs-sync marker**（仅 Harness 项目）
- 检查 `.harness/docs-sync-marker.json` 中的 `docs_synced` 字段
- `docs_synced: true` → 允许提交
- `docs_synced: false` → 阻止提交，提示先同步文档

**工作流程：**
1. Agent 完成切片开发（写代码 → 跑测试 → 测试通过）
2. 立即同步文档（api-changes.md / ddl-changes.md / tech-design.md）
3. 更新 `.harness/docs-sync-marker.json`：设置 `docs_synced: true`
4. 提交代码（pre-commit hook 放行）

**详见：**
- `flow/skill.md` → "文档同步机制" 章节
- `flow/phase-3-spec-dev/flow.md` → "切片完成 checklist"
- `core-design/templates/docs-sync-marker.json` → 标记文件模板

## 软链接约定

- Git hooks：`ln -sf ../../.harness/hooks/git/<hook> .git/hooks/<hook>`
- AI 工具 hooks：待工具 hook 目录确定后补充
