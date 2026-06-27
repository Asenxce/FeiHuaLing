#!/bin/bash
# ============================================================
# Git Hooks 安装脚本
# 安装后自动实现：本地明文 → 提交时占位符 → 推送到 GitHub 安全
#
# 使用方法:
#   export DB_PASSWORD="你的密码"
#   export OSS_ACCESS_KEY="你的Key"
#   export OSS_ACCESS_SECRET="你的Secret"
#   bash setup-hooks.sh
#
# 或交互式输入（不设置环境变量时会提示）
# ============================================================

set -e

HOOKS_DIR=".git/hooks"

if [ ! -d "$HOOKS_DIR" ]; then
  echo "错误: 请在 Git 仓库根目录下运行此脚本"
  exit 1
fi

get_value() {
  local var_name="$1"
  local prompt="$2"
  local value="${!var_name}"
  if [ -z "$value" ]; then
    read -r -p "$prompt: " value
  fi
  echo "$value"
}

echo "============================================"
echo "  Git Hooks 安装 - 敏感信息自动脱敏"
echo "============================================"
echo ""

DB_PASS=$(get_value "DB_PASSWORD" "数据库密码")
OSS_KEY=$(get_value "OSS_ACCESS_KEY" "阿里云OSS AccessKey ID")
OSS_SECRET=$(get_value "OSS_ACCESS_SECRET" "阿里云OSS AccessKey Secret")

# 生成 pre-commit hook（使用 sed + tmp 避免跨平台兼容问题）
cat > "$HOOKS_DIR/pre-commit" << HOOK_EOF
#!/bin/sh
# Pre-commit: 明文密码 → 占位符，保护 GitHub 仓库
# 本地文件始终保留明文，提交时自动替换

replace_in_file() {
    file="\$1"
    real="\$2"
    placeholder="\$3"
    sed "s/\$real/\$placeholder/g" "\$file" > "\$file.tmp" && mv "\$file.tmp" "\$file"
}

FILES="backend/feihualingbattle/src/main/resources/application.properties
backend/feihualingbattle/import_poetry.py"

CHANGED=false

for file in \$FILES; do
    if git diff --cached --name-only | grep -qxF "\$file"; then
        echo "[pre-commit] 处理: \$file"

        replace_in_file "\$file" "$DB_PASS"  "your_db_password"
        replace_in_file "\$file" "$OSS_KEY"  "your_access_key_id"
        replace_in_file "\$file" "$OSS_SECRET" "your_access_key_secret"

        git add "\$file"
        CHANGED=true
    fi
done

if [ "\$CHANGED" = true ]; then
    echo "\$FILES" | tr '\n' ' ' > .githooks/.restore_flag
    echo "[pre-commit] 已替换敏感值为占位符"
fi

exit 0
HOOK_EOF

# 生成 post-commit hook
cat > "$HOOKS_DIR/post-commit" << HOOK_EOF
#!/bin/sh
# Post-commit: 占位符 → 明文密码，恢复本地文件

restore_in_file() {
    file="\$1"
    placeholder="\$2"
    real="\$3"
    sed "s/\$placeholder/\$real/g" "\$file" > "\$file.tmp" && mv "\$file.tmp" "\$file"
}

FLAG=".githooks/.restore_flag"

if [ -f "\$FLAG" ]; then
    FILES=\$(cat "\$FLAG")
    for file in \$FILES; do
        if [ -f "\$file" ]; then
            echo "[post-commit] 恢复: \$file"
            restore_in_file "\$file" "your_db_password"       "$DB_PASS"
            restore_in_file "\$file" "your_access_key_id"     "$OSS_KEY"
            restore_in_file "\$file" "your_access_key_secret" "$OSS_SECRET"
        fi
    done
    rm -f "\$FLAG"
    echo "[post-commit] 敏感值已恢复为明文"
fi

exit 0
HOOK_EOF

chmod +x "$HOOKS_DIR/pre-commit" "$HOOKS_DIR/post-commit"

echo ""
echo "============================================"
echo "  Git Hooks 安装完成!"
echo "  pre-commit  → 提交前自动替换敏感值"
echo "  post-commit → 提交后自动恢复明文"
echo "============================================"
