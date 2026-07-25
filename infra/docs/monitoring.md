## Monitoring

이 문서는 EC2 host metric 수집과 시각화 구성을 정리합니다.

구성 흐름은 아래와 같습니다.

```text
Node Exporter -> Prometheus -> Grafana -> User
```

Node Exporter, Prometheus, Grafana는 EC2 host 기준 `127.0.0.1` 접근이 필요하므로 Linux Docker에서 `network_mode: host`를 사용합니다.

## Node Exporter

Node Exporter는 EC2 host의 CPU, memory, disk, network metric을 Prometheus 형식으로 노출합니다.

실행:

```bash
sh infra/scripts/up-node-exporter.sh
```

검증:

```bash
docker compose -p s14p31s207-node-exporter \
  -f infra/compose/docker-compose.node-exporter.yml \
  ps

curl -fsS http://127.0.0.1:9100/metrics | head
curl -fsS http://127.0.0.1:9100/metrics | grep '^node_cpu_seconds_total' | head
```

Windows tunnel 확인:

```text
http://localhost:19100/metrics
```

## Prometheus

Prometheus는 Node Exporter metric을 scrape하고 PromQL 질의 endpoint를 제공합니다.

설정 파일:

```text
infra/prometheus/prometheus.yml
```

핵심 scrape target:

```yaml
scrape_configs:
  - job_name: node
    static_configs:
      - targets:
          - 127.0.0.1:9100
```

실행:

```bash
sh infra/scripts/up-prometheus.sh
```

검증:

```bash
docker compose -p s14p31s207-prometheus \
  -f infra/compose/docker-compose.prometheus.yml \
  ps

curl -fsS http://127.0.0.1:9090/-/healthy
curl -fsS 'http://127.0.0.1:9090/api/v1/query?query=up'
```

브라우저 접속:

```text
http://localhost:19090
```

자주 쓰는 PromQL:

```promql
100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)
```

```promql
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100
```

```promql
(1 - (node_filesystem_avail_bytes{mountpoint="/",fstype!="rootfs"} / node_filesystem_size_bytes{mountpoint="/",fstype!="rootfs"})) * 100
```

```promql
node_load1
```

## Grafana

Grafana는 Prometheus datasource를 통해 EC2 metric dashboard를 구성합니다.

환경 변수:

```dotenv
GRAFANA_ADMIN_USER=s207-admin
GRAFANA_ADMIN_PASSWORD=change-this-grafana-password
```

실행:

```bash
sh infra/scripts/up-grafana.sh
```

검증:

```bash
docker compose -p s14p31s207-grafana \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.grafana.yml \
  ps

curl -fsS http://127.0.0.1:3000/api/health
```

브라우저 접속:

```text
http://localhost:13000
```

Datasource:

```text
Name: Prometheus
URL: http://127.0.0.1:9090
```

권장 dashboard panel:

| Panel | PromQL |
| :-- | :-- |
| CPU usage % | `100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)` |
| Memory usage % | `(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100` |
| Disk usage % | `(1 - (node_filesystem_avail_bytes{mountpoint="/",fstype!="rootfs"} / node_filesystem_size_bytes{mountpoint="/",fstype!="rootfs"})) * 100` |
| Load 1m | `node_load1` |
