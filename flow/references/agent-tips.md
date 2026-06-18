# Agent 实战踩坑笔记

> 这些不是流程规范，是执行 Harness 流程时的技术陷阱。后续 Agent 执行前过一遍。

## 文档铁律

- **Markdown 是源码，HTML 是视图。** 改流程时先改 md，再改 html，不要反过来。
- 在 html 上迭代半天然后"同步回 md"，必然遗漏。改完后做一次两文件内容交叉核对。

## Patch 工具换行陷阱

- `patch` 工具在 `new_string` / `old_string` 中的真实换行符可能被转义为 `\n` 写入文件。
- 受影响的场景：代码块（``` 包裹的内容）、ASCII 图表、多行文本块。
- **修复方法：** 用 `execute_code` + Python 的 `read_file` / `write_file` 直接操作文件内容。

## Cron 脚本路径约束

- Hermes cron 的 `script` 参数必须是**相对于 `~/.hermes/scripts/` 的路径**，不接受绝对路径。
- 如果你想把脚本放在别处（如 git repo），用软链：`ln -s /actual/path/script.py ~/.hermes/scripts/script.py`

## GitHub CLI 授权

- `gh auth login --web` 在非 TTY 环境下输出到 stderr，可能被吞。
- **推荐：** `gh auth login --hostname github.com --web > /tmp/gh-out.txt 2>&1`，然后 `cat /tmp/gh-out.txt` 获取 device code。
- Device flow 授权完成后进程自动退出，用 `gh auth status` 确认。

## 目录结构约定

- Harness 所有内容归到 `~/docs/harness-research/`。
- 可执行 Skill 在 `dev-harness-skill/`，按 4 阶段分目录。
- 跨阶段共享资源放 `shared/`。
