# 다시봄, 한국

한국관광공사 관광사진·관광지·관광지 집중률 예측으로 지역 여행을 발견하는 웹 서비스.

공모전 OpenAPI 활용 방식과 화면별 호출은 [`docs/api-compliance.md`](docs/api-compliance.md)에 정리했습니다.

## 제공 기능

- 공식 코드표 기준 전국 252개 시군구 검색과 전국 시군구 경계 지도
- 인구감소지역 89곳, 관심지역 18곳, 현재 반값여행 25곳 필터와 방문량 기반 숨은 점수
- 방문량이 낮은 정책지역 중 관광사진 수상작을 보유한 후보를 날짜별로 선정하는 오늘의 동네
- 관광 서비스 12개와 문화자원 5개로 구성한 17개 매력 지표 레이더 차트
- 관광지·음식점·숙박을 각각 최대 50곳까지 조회수 기준으로 큐레이션
- 관광사진 수상작 우선 갤러리와 IntersectionObserver 기반 무한 스크롤
- 향후 30일 관광지 집중률 히트맵과 가장 한적한 날짜 3개 추천
- 브라우저 여행 저장, 익명 좋아요/취소, 카카오 SDK 연동 및 네이티브 공유, 지역별 Open Graph
- 화면 요청 시 공공 API 실시간 호출, 공공 API 응답 DB 저장·장기 캐시 미사용

자원 수요 API가 빈 결과를 반환하면 17개 지표를 임의로 생성하지 않고 원천 데이터 대기 상태를 표시합니다. 반값여행 참여지역 수는 제안서 작성 당시 16곳에서 현재 공식 페이지 25곳으로 확대된 값을 반영했습니다.

## 개발

Node.js 22+, Java 21, Docker가 필요합니다.

1. `infra/.env.example`을 `infra/.env`로 복사하고 DB 비밀번호를 설정합니다.
2. `docker compose --env-file infra/.env -f infra/compose.yaml up -d`
3. `backend/.env.example`을 `backend/.env`로 복사해 DB 설정과 TourAPI 디코딩 키를 설정합니다.
4. `bash scripts/backend-dev.sh`
5. `cd frontend && npm ci && npm run dev`

관광사진·관광지·집중률은 지역 화면 요청 시 백엔드가 TourAPI를 실시간 호출합니다. 서비스키는 브라우저에 전달하지 않으며 공공 API 응답은 MySQL에 저장하지 않습니다. MySQL은 공식 지역 코드와 익명 좋아요 등 서비스 자체 데이터에만 사용합니다.

## 검증

```sh
cd backend && ./gradlew test bootJar
cd ../frontend && npm run lint && npm test && npm run build -- --webpack
PLAYWRIGHT_BASE_URL=https://your-domain.example npm run test:e2e
```

백엔드 통합 테스트는 Testcontainers의 독립 MySQL을 사용합니다. E2E는 TourAPI 운영계정이 연결된 서버에서 데스크톱·모바일의 검색/상세/저장을 확인합니다. 공개 서버 테스트에서 생성한 좋아요는 테스트 종료 시 취소합니다.

## EC2 운영 배포

`infra/compose.production.yaml`: MySQL + Spring Boot + Next.js + Caddy. 빌드는 로컬에서 완료하고 실행 이미지만 전송합니다. Caddy가 인증서를 발급·갱신하고 HTTP를 HTTPS로 연결합니다.

1. 전용 Amazon Linux 2023 EC2에서 `scripts/bootstrap-server.sh` 실행: Docker, Compose, 2GB 스왑.
2. `/opt/dasibom/.env.production`을 예제에 맞게 작성하고 권한을 `600`으로 설정합니다. 실제 키는 저장소에 넣지 않습니다.
3. `SSH_KEY=/path/to/key.pem SITE_HOST=your-domain.example bash scripts/deploy.sh ec2-user@host release-tag`

### 비용과 가용성

- 서울 `t3.micro`, gp3 8GiB, CPU credit standard. 앱/DB는 외부에 포트를 노출하지 않습니다.
- 80/443만 공개하고 SSH는 관리자의 현재 IP만 허용합니다.
- JVM/Node/MySQL 메모리 상한, 로그 파일당 5MB × 2개, 자동 재시작을 적용합니다.
- 1GB 서버는 심사용 소규모 동시 접속을 목표로 합니다. 스왑은 메모리 급증 완충용이며 RAM의 대체물이 아닙니다.
- 도메인 미보유 시 sslip.io의 IP 기반 호스트를 사용할 수 있습니다. 외부 DNS 서비스에 의존하며, EC2 중지/시작으로 공인 IP가 바뀌면 URL·인증서·Origin 설정을 함께 변경해야 합니다. 심사 기간에는 인스턴스를 계속 실행하세요.
- 데이터베이스에는 공공 API 응답을 저장하지 않으며 지역 코드와 익명 좋아요만 Docker 볼륨에 보존됩니다. `docker compose down -v`는 이 데이터를 삭제하므로 사용하지 마세요. 현재 단일 서버 구성은 고가용성/외부 백업을 제공하지 않습니다.
- 무료 플랜의 크레딧/만료일은 AWS 결제 콘솔에서 확인하세요. 비용 예산 알림은 실제 사용을 차단하는 한도가 아닙니다.

### 점검/복구

```sh
cd /opt/dasibom
sudo docker compose --env-file .env.production -f compose.production.yaml ps
sudo docker compose --env-file .env.production -f compose.production.yaml logs --tail 80 backend frontend caddy
curl -f https://your-domain.example/api/health
```

이전 이미지 태그가 남아 있다면 `.env.production`의 `RELEASE`를 이전 값으로 바꾸고 `up -d --wait`로 되돌릴 수 있습니다. TourAPI가 빈 응답이나 오류를 반환하면 임의 데이터 대신 해당 화면의 안내 상태를 표시합니다.
