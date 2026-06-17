#!/usr/bin/env python3
"""
模板参数化 —— 将模板中的占位符替换为实际项目值。

在 harver-java-init (SKILL.md) 的 Step 8「复制基础设施代码」之后运行。

占位符约定：
  com.example     → 实际包名（如 com.fshows.farvis）
  app_db          → 实际数据库名
  Resource        → 实际聚合根名（Agent 每次使用时自行替换，不在此脚本范围）

使用方式：
  python3 parameterize.py \\
    --base-package com.fshows.farvis \\
    --db-name farvis_db \\
    --target-dir src/

修改范围：
  - 所有 .java 文件中的 package 声明和 import
  - application-*.yml 中的 spring.datasource.url
  - build.gradle.kts 中的 group
  - .harness/ 下的 yaml 文件中的包名引用
"""

import argparse
import os
import re
import sys
from pathlib import Path

# 需要替换包名的文件扩展名
JAVA_EXTENSIONS = {'.java', '.kt'}
CONFIG_EXTENSIONS = {'.yml', '.yaml', '.kts', '.properties', '.xml'}
CONTEXT_EXTENSIONS = {'.yaml', '.yml', '.md'}

PLACEHOLDER_PACKAGE = 'com.example'
PLACEHOLDER_DB = 'app_db'
PLACEHOLDER_GROUP = 'com.example'


def replace_in_file(filepath: Path, old: str, new: str) -> bool:
    """替换文件中的字符串，返回是否做了修改。"""
    try:
        content = filepath.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        return False

    if old not in content:
        return False

    new_content = content.replace(old, new)
    filepath.write_text(new_content, encoding='utf-8')
    return True


def parameterize_package(target_dir: Path, base_package: str) -> int:
    """替换所有文件中的 com.example → base_package。"""
    count = 0

    for root, dirs, files in os.walk(target_dir):
        for filename in files:
            filepath = Path(root) / filename
            ext = filepath.suffix.lower()

            # 跳过 build/ .gradle/ .git/ node_modules/
            if any(skip in str(filepath) for skip in ['/build/', '/.gradle/', '/.git/', '/node_modules/']):
                continue

            should_replace = False
            if ext in JAVA_EXTENSIONS:
                should_replace = True
            elif ext in CONFIG_EXTENSIONS:
                should_replace = True
            elif ext in CONTEXT_EXTENSIONS:
                should_replace = True
            elif filename == 'Dockerfile':
                should_replace = True

            if should_replace and replace_in_file(filepath, PLACEHOLDER_PACKAGE, base_package):
                count += 1
                print(f"  📦 {filepath.relative_to(target_dir)}")

    return count


def parameterize_db(target_dir: Path, db_name: str) -> int:
    """替换配置文件中的数据库名。"""
    count = 0

    for root, dirs, files in os.walk(target_dir):
        for filename in files:
            filepath = Path(root) / filename
            if filepath.suffix.lower() in CONFIG_EXTENSIONS:
                if replace_in_file(filepath, PLACEHOLDER_DB, db_name):
                    count += 1
                    print(f"  🗄️  {filepath.relative_to(target_dir)}")

    return count


def main():
    parser = argparse.ArgumentParser(description='参数化 Java Harness 模板')
    parser.add_argument('--base-package', required=True, help='实际包名，如 com.fshows.farvis')
    parser.add_argument('--db-name', default=None, help='数据库名，默认不修改（保持 app_db）')
    parser.add_argument('--target-dir', default='src', help='目标目录，默认 src/')
    parser.add_argument('--dry-run', action='store_true', help='只检查不修改')
    args = parser.parse_args()

    target_dir = Path(args.target_dir)
    if not target_dir.exists():
        print(f"❌ 目标目录不存在: {target_dir}")
        sys.exit(1)

    base_package = args.base_package.strip()

    if args.dry_run:
        print(f"🔍 预览模式 —— 将替换 '{PLACEHOLDER_PACKAGE}' → '{base_package}'")
        print(f"   范围: {target_dir.resolve()}")
        print()

        preview_count = 0
        for root, dirs, files in os.walk(target_dir):
            for filename in files:
                filepath = Path(root) / filename
                try:
                    content = filepath.read_text(encoding='utf-8')
                except UnicodeDecodeError:
                    continue
                if PLACEHOLDER_PACKAGE in content:
                    preview_count += 1
                    print(f"  → {filepath.relative_to(target_dir)}")

        print(f"\n共 {preview_count} 个文件将被修改。")
        if preview_count > 0:
            print("去掉 --dry-run 执行实际替换。")
        return

    print(f"🔧 参数化: {PLACEHOLDER_PACKAGE} → {base_package}")
    print(f"   范围: {target_dir.resolve()}")
    print()

    # 1. 替换包名
    pkg_count = parameterize_package(target_dir, base_package)

    # 2. 替换数据库名（如果指定）
    db_count = 0
    if args.db_name:
        print(f"\n🔧 数据库: {PLACEHOLDER_DB} → {args.db_name}")
        db_count = parameterize_db(target_dir, args.db_name)

    # 3. 如有 .harness/ 目录也处理
    harness_dir = target_dir.parent / '.harness'
    if harness_dir.exists():
        print(f"\n🔧 参数化 .harness/ 上下文文件")
        h_count = parameterize_package(harness_dir, base_package)
        pkg_count += h_count

    print(f"\n✅ 完成: {pkg_count} 个文件替换了包名" + (f", {db_count} 个文件替换了数据库名" if db_count else ""))


if __name__ == '__main__':
    main()
