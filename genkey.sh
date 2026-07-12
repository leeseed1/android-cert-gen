#!/bin/bash

# 确保脚本以 root 权限运行
if [ "$EUID" -ne 0 ]; then
  echo "错误: 请使用 root 权限运行此脚本 (sudo -i)。"
  exit 1
fi

# 交互式输入 IP 或域名
read -p "请输入您的 IP 地址或域名: " TARGET_HOST

if [ -z "$TARGET_HOST" ]; then
  echo "错误: 输入不能为空！"
  exit 1
fi

# 定义保存目录（根目录下的 selfsign）
TARGET_DIR="/selfsign"
mkdir -p "$TARGET_DIR"

# 临时生成 openssl 配置文件
# 核心修改：在证书中同时注入服务器域名(SAN)和CA属性(CA:true)，使其能独立导入手机并生效
CONF_FILE=$(mktemp)
cat <<EOF > "$CONF_FILE"
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_independent
prompt = no

[req_distinguished_name]
CN = $TARGET_HOST

[v3_independent]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true
keyUsage = critical, digitalSignature, keyEncipherment, cRLSign, keyCertSign
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
EOF

# 自动判断输入的是 IP 还是域名，并写入 SAN
if [[ "$TARGET_HOST" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "IP.1 = $TARGET_HOST" >> "$CONF_FILE"
else
  echo "DNS.1 = $TARGET_HOST" >> "$CONF_FILE"
fi

echo "--------------------------------------------------"
echo "正在开始生成独立自签证书（支持直接导入手机），保存目录: $TARGET_DIR"
echo "--------------------------------------------------"

# ==================== 1. 独立 RSA 2048 证书 ====================
echo "[1/3] 正在生成独立 RSA 2048 证书..."
RSA_KEY="$TARGET_DIR/rsa_2048.key"
RSA_CERT="$TARGET_DIR/rsa_2048.crt"

openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
  -keyout "$RSA_KEY" -out "$RSA_CERT" \
  -config "$CONF_FILE" -extensions v3_independent 2>/dev/null

echo "--> RSA 2048 SHA-256 指纹:"
openssl x509 -noout -fingerprint -sha256 -in "$RSA_CERT"
echo ""

# ==================== 2. 独立 ECDSA prime256v1 证书 ====================
echo "[2/3] 正在生成独立 ECDSA (prime256v1) 证书..."
ECDSA_KEY="$TARGET_DIR/ecdsa_p256.key"
ECDSA_CERT="$TARGET_DIR/ecdsa_p256.crt"

openssl req -x509 -nodes -days 3650 -newkey ec:<(openssl ecparam -name prime256v1) \
  -keyout "$ECDSA_KEY" -out "$ECDSA_CERT" \
  -config "$CONF_FILE" -extensions v3_independent 2>/dev/null

echo "--> ECDSA prime256v1 SHA-256 指纹:"
openssl x509 -noout -fingerprint -sha256 -in "$ECDSA_CERT"
echo ""

# ==================== 3. 独立 Ed25519 证书 ====================
echo "[3/3] 正在生成独立 Ed25519 证书..."
ED_KEY="$TARGET_DIR/ed25519.key"
ED_CERT="$TARGET_DIR/ed25519.crt"

# 分步执行以确保老版本 OpenSSL 的兼容性，同时赋予其自签 CA 属性
openssl genpkey -algorithm ed25519 -out "$ED_KEY" 2>/dev/null
openssl req -x509 -nodes -days 3650 -key "$ED_KEY" -out "$ED_CERT" \
  -config "$CONF_FILE" -extensions v3_independent 2>/dev/null

echo "--> Ed25519 SHA-256 指纹:"
openssl x509 -noout -fingerprint -sha256 -in "$ED_CERT"
echo ""

# 清理临时配置文件
rm -f "$CONF_FILE"

echo "--------------------------------------------------"
echo "所有独立证书已成功生成！"
echo "使用说明：如果您在 VPS 服务端配置了哪个证书，"
echo "就请直接将对应的 .crt 文件发送到手机端导入并信任。"
echo "--------------------------------------------------"
