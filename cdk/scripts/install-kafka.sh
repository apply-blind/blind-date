#!/bin/bash
set -euxo pipefail

# 인자 파싱
KAFKA_PORT=${1:-9092}
ZOOKEEPER_PORT=${2:-2181}

# Private IP 획득 (EC2 Metadata Service)
PRIVATE_IP=$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4)

# Docker 설치
echo "Installing Docker..."
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# netcat 설치 (Healthcheck용)
yum install -y nmap-ncat

# Docker Compose 설치
echo "Installing Docker Compose..."
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Kafka docker-compose.yml 생성
mkdir -p /home/ec2-user/kafka
cat > /home/ec2-user/kafka/docker-compose.yml <<EOF
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: blind-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: ${ZOOKEEPER_PORT}
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "${ZOOKEEPER_PORT}:2181"
    healthcheck:
      test: nc -z localhost 2181 || exit 1
      interval: 10s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: blind-kafka
    depends_on:
      zookeeper:
        condition: service_healthy
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://${PRIVATE_IP}:${KAFKA_PORT}
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    ports:
      - "${KAFKA_PORT}:9092"
    healthcheck:
      test: kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1
      interval: 10s
      retries: 10
      start_period: 30s
EOF

chown -R ec2-user:ec2-user /home/ec2-user/kafka

# systemd 서비스 등록 (EC2 재부팅 시 자동 시작)
cat > /etc/systemd/system/kafka.service <<EOF
[Unit]
Description=Kafka Docker Compose
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ec2-user/kafka
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
User=ec2-user

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable kafka.service
systemctl start kafka.service

# Kafka 준비 완료 대기
echo "Waiting for Kafka to be ready..."
until docker exec blind-kafka kafka-broker-api-versions --bootstrap-server localhost:9092; do
  echo "Kafka not ready, retrying..."
  sleep 5
done

echo "✅ Kafka is ready! Endpoint: ${PRIVATE_IP}:${KAFKA_PORT}"