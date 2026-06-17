# AI 编码工具 Hooks

此目录存放各 AI 编码工具的 hook 脚本。

当前状态：各工具的 hook 机制尚未确定，目录为占位。
确定后在此放置脚本，初始化时同样通过软链接接入工具目录。

## 目录约定

```
hooks/
├── git/                   # Git hooks（源文件）
│   ├── pre-commit         # 提交前 fastTest
│   ├── pre-push           # 推送前全量测试
│   └── commit-msg         # 提交信息格式校验
├── claude/                # Claude Code hooks（占位）
├── codex/                 # Codex hooks（占位）
└── qoder/                 # Qoder hooks（占位）
```

## 软链接约定

- Git hooks：`ln -sf ../../.harness/hooks/git/<hook> .git/hooks/<hook>`
- AI 工具 hooks：待工具 hook 目录确定后补充
