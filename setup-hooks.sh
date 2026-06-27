#!/bin/bash
# ============================================================
# Git Hooks 安装脚本
# 安装 pre-commit / post-commit 钩子，实现：
#   本地文件保留明文密码 → 提交时自动替换为占位符 → GitHub 仓库安全
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

# 检查是否在 Git 仓库根目录
if [ ! -d "$HOOKS_DIR" ]; then
  echo "错误: 请在 Git 仓库根目录下运行此脚本"
  exit 1
fi

# 获取敏感值（环境变量 > 用户输入）
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
echo "  Git Hooks 安装 - 敏感信息保护"
echo "============================================"
echo ""
echo "该脚本将安装 pre-commit 和 post-commit 钩子。"
echo "钩子会在提交时自动将密码/密钥替换为占位符，"
echo "提交完成后自动恢复本地明文。"
echo ""

DB_PASS=$(get_value "DB_PASSWORD" "数据库密码")
OSS_KEY=$(get_value "OSS_ACCESS_KEY" "阿里云OSS AccessKey ID")
OSS_SECRET=$(get_value "OSS_ACCESS_SECRET" "阿里云OSS AccessKey Secret")

# 生成 pre-commit hook
cat > "$HOOKS_DIR/pre-commit" << HOOK_EOF
#!/bin/bash
set -e

declare -A REPLACE_MAP=(
  ["$DB_PASS"]="your_db_password"
  ["$OSS_KEY"]="your_access_key_id"
  ["$OSS_SECRET"]="your_access_key_secret"
)

FILES=(
  "backend/feihualingbattle/src/main/resources/application.properties"
  "backend/feihualingbattle/import_poetry.py"
)

CHANGED=false
for file in "\${FILES[@]}"; do
  if git diff --cached --name-only | grep -qFx "\$file"; then
    echo "[pre-commit] 处理敏感文件: \$file"
    for real in "\${!REPLACE_MAP[@]}"; do
      placeholder="\${REPLACE_MAP[\$real]}"
      sed -i "s/\$real/\$placeholder/g" "\$file"
    done
    git add "\$file"
    CHANGED=true
  fi
done

if [ "\$CHANGED" = true ]; then
  echo "\${FILES[*]}" > .githooks/.restore_flag
fi
exit 0
HOOK_EOF

# 生成 post-commit hook
cat > "$HOOKS_DIR/post-commit" << HOOK_EOF
#!/bin/bash
set -e

declare -A RESTORE_MAP=(
  ["your_db_password"]="$DB_PASS"
  ["your_access_key_id"]="$OSS_KEY"
  ["your_access_key_secret"]="$OSS_SECRET"
)

FLAG_FILE=".githooks/.restore_flag"
if [ -f "\$FLAG_FILE" ]; then
  FILES=\$(cat "\$FLAG_FILE")
  for file in \$FILES; do
    for placeholder in "\${!RESTORE_MAP[@]}"; do
      real="\${RESTORE_MAP[\$placeholder]}"
      sed -i "s/\$placeholder/\$real/g" "\$file"
    done
  done
  rm -f "\$FLAG_FILE"
fi
exit 0
HOOK_EOF

# 设置可执行权限
chmod +x "$HOOKS_DIR/pre-commit" "$HOOKS_DIR/post-commit"

echo ""
echo "钩子安装完成!"
echo "  pre-commit  → 提交前自动替换敏感值"
echo "  post-commit → 提交后自动恢复明文"
