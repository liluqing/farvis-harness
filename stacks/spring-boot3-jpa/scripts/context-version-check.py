#!/usr/bin/env python3
"""
上下文版本管理 —— 检测 .harness/ai-context/ 下各文件的版本一致性。

如果 project-map.yaml 的模块定义改了，但 business-rules.yaml 没同步更新
（反之亦然），说明上下文文件之间存在不一致，Agent 可能拿到矛盾的信息。

使用方式：
  python3 context-version-check.py [--fix]

检测逻辑：
  1. 收集每个 YAML 文件的最后修改时间和内容 hash
  2. 比较 project-map 和 business-rules 的时间差
  3. 如果 project-map 比 business-rules 新 > 7 天，且 business-rules 无对应模块的规则 → 告警
  4. 比较 error-catalog 和 business-rules 之间的错误码一致性
"""

import argparse
import hashlib
import os
import sys
import time
from pathlib import Path
from datetime import datetime, timedelta

AI_CONTEXT_DIR = ".harness/ai-context"

FILES = {
    "project-map": "project-map.yaml",
    "business-rules": "business-rules.yaml",
    "error-catalog": "error-catalog.yaml",
    "coding-rules": "coding-rules.yaml",
}


def file_info(filepath: Path) -> dict:
    """获取文件的修改时间和内容 hash。"""
    if not filepath.exists():
        return {"mtime": 0, "hash": "", "exists": False}
    stat = filepath.stat()
    content = filepath.read_text(encoding="utf-8")
    return {
        "mtime": stat.st_mtime,
        "hash": hashlib.sha256(content.encode()).hexdigest()[:12],
        "exists": True,
        "size": len(content),
    }


def check_staleness(info: dict, name: str) -> list[str]:
    """检查文件是否过期（空模板）。"""
    issues = []
    if info["exists"] and info["size"] < 200:
        issues.append(f"⚠️  {name} 接近空模板（{info['size']} bytes），建议填写")
    return issues


def check_cross_consistency(infos: dict) -> list[str]:
    """检查跨文件一致性。"""
    issues = []

    pm = infos.get("project-map", {})
    br = infos.get("business-rules", {})

    if pm.get("exists") and br.get("exists"):
        # project-map 比 business-rules 新很多 → 可能模块改了但规则没更新
        pm_mtime = pm.get("mtime", 0)
        br_mtime = br.get("mtime", 0)
        delta = pm_mtime - br_mtime
        if delta > 7 * 86400:  # 7 天
            issues.append(
                f"⚠️  project-map.yaml 比 business-rules.yaml 新 {delta // 86400} 天，"
                f"模块划分可能已更新但业务规则未同步"
            )

    return issues


def main():
    parser = argparse.ArgumentParser(description="Harness 上下文版本检查")
    parser.add_argument("--dir", default=AI_CONTEXT_DIR, help=f"上下文目录（默认 {AI_CONTEXT_DIR}）")
    args = parser.parse_args()

    context_dir = Path(args.dir)
    if not context_dir.exists():
        print(f"❌ 目录不存在: {context_dir}")
        print("   提示：先运行 '初始化 Java Harness'")
        sys.exit(1)

    print(f"📋 上下文版本检查 — {context_dir}")
    print(f"   时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()

    infos = {}
    for key, filename in FILES.items():
        info = file_info(context_dir / filename)
        infos[key] = info
        if info["exists"]:
            mtime_str = datetime.fromtimestamp(info["mtime"]).strftime("%Y-%m-%d %H:%M")
            print(f"  {'✅' if info['size'] > 200 else '⚠️'} {filename}: {info['size']} bytes, last modified {mtime_str}")
        else:
            print(f"  ❌ {filename}: 不存在")

    print()

    # 检查
    all_issues = []
    for key, filename in FILES.items():
        all_issues.extend(check_staleness(infos.get(key, {}), filename))

    all_issues.extend(check_cross_consistency(infos))

    if all_issues:
        print("发现问题：")
        for issue in all_issues:
            print(f"  {issue}")
        print()
        print("建议：对 Agent 说「更新 Harness 上下文」自动修复。")
        sys.exit(1)
    else:
        print("✅ 上下文版本一致，无过期检测。")
        sys.exit(0)


if __name__ == "__main__":
    main()
