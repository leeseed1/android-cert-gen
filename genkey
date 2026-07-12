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

# 临时生成服务器证书的 openssl 配置文件 (用于支持 SAN 扩展)
CONF_FILE=$(mktemp)
cat <<EOF > "$CONF_FILE"
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no
[req_distinguished_name]
CN = $TARGET_HOST
[v3_req]
keyUsage = digitalSignature, keyEncipherment
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
echo " 正在初始化专属 CA 根证书 (手机端导入此证书) "
echo "--------------------------------------------------"

CA_KEY="$TARGET_DIR/MyPrivateCA.key"
CA_CERT="$TARGET_DIR/MyPrivateCA.crt"

# 生成 CA 根证书的临时配置
CA_CONF=$(mktemp)
cat <<EOF > "$CA_CONF"
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_ca
prompt = no
[req_distinguished_name]
CN = My Personal Private CA
[v3_ca]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true
keyUsage = critical, digitalSignature, cRLSign, keyCertSign
EOF

# 生成 CA 私钥和证书 (有效期 10 年)
openssl req -x509 -nodes -days 3650 -newkey rsa:3072 \
  -keyout "$CA_KEY" -out "$CA_CERT" \
  -config "$CA_CONF" 2>/dev/null

echo " CA 根证书已生成 -> $CA_CERT"
echo "--------------------------------------------------"
echo "正在开始生成 3 种服务器证书..."
echo "--------------------------------------------------"

# ==================== 1. RSA 2048 证书 ====================
echo "[1/3] 正在生成并签发 RSA 2048 证书..."
RSA_KEY="$TARGET_DIR/rsa_2048.key"
RSA_CSR=$(mktemp)
RSA_CERT="$TARGET_DIR/rsa_2048.crt"

openssl req -new -nodes -newkey rsa:2048 -keyout "$RSA_KEY" -out "$RSA_CSR" -config "$CONF_FILE" 2>/dev/null
openssl x509 -req -days 3650 -in "$RSA_CSR" -CA "$CA_CERT" -CAkey "$CA_KEY" -CAcreateserial \
  -out "$RSA_CERT" -extfile "$CONF_FILE" -extensions v3_req 2>/dev/null

echo "--> RSA 2048 SHA-256 指纹:"
openssl x509 -noout -fingerprint -sha256 -in "$RSA_CERT"
echo ""

# ==================== 2. ECDSA prime256v1 证书 ====================
echo "[2/3] 正在生成并签发 ECDSA (prime256v1) 证书..."
ECDSA_KEY="$TARGET_DIR/ecdsa_p256.key"
ECDSA_CSR=$(mktemp)
ECDSA_CERT="$TARGET_DIR/ecdsa_p256.crt"

openssl req -new -nodes -newkey ec:<(openssl ecparam -name prime256v1) -keyout "$ECDSA_KEY" -out "$ECDSA_CSR" -config "$CONF_FILE" 2>/dev/null
openssl x509 -req -days 3650 -in "$ECDSA_CSR" -CA "$CA_CERT" -CAkey "$CA_KEY" -CAcreateserial \
  -out "$ECDSA_CERT" -extfile "$CONF_FILE" -extensions v3_req 2>/dev/null

echo "--> ECDSA prime256v1 SHA-256 指纹:"
openssl x509 -noout -fingerprint -sha256 -in "$ECDSA_CERT"
echo ""

# ==================== 3. Ed25519 证书 ====================
echo "[3/3] 正在生成并签发 Ed25519 证书..."
ED_KEY="$TARGET_DIR/ed25519.key"
ED_CSR=$(mktemp)
ED_CERT="$TARGET_DIR/ed25519.crt"

openssl genpkey -algorithm ed25519 -out "$ED_KEY" 2>/dev/null
openssl req -new -key "$ED_KEY" -out "$ED_CSR" -config "$CONF_FILE" 2>/dev/null
openssl x509 -req -days 3650 -in "$ED_CSR" -CA "$CA_CERT" -CAkey "$CA_KEY" -CAcreateserial \
  -out "$ED_CERT" -extfile "$CONF_FILE" -extensions v3_req 2>/dev/null

echo "--> Ed25519 SHA-256 指纹:"
openssl x509 -noout -fingerprint -sha256 -in "$ED_CERT"
echo ""

# 清理临时文件
rm -f "$CONF_FILE" "$CA_CONF" "$RSA_CSR" "$ECDSA_CSR" "$ED_CSR" "$TARGET_DIR"/*.srl

echo "--------------------------------------------------"
echo " 恭喜！所有证书已成功签发！"
echo " 手机端导入指南: 请下载并安装 /selfsign/MyPrivateCA.crt"
echo "--------------------------------------------------"
