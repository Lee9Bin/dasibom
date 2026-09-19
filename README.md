# 다시봄, 한국

한국관광공사 관광사진·관광지·관광지 집중률 예측으로 지역 여행을 발견하는 웹 서비스.

## 제공 기능

- 8개 에디터 선정 지역, 한국 시간 날짜에 따른 오늘의 발견
- 지역명·시도·테마 검색, 지도와 목록 탐색
- 실제 TourAPI 사진 갤러리, 촬영자·촬영월·출처, 사진 더 보기
- 지역 관광지와 지도 링크, 향후 집중률과 낮은 날짜 3개 추천
- 브라우저 여행 저장, 익명 좋아요/취소, 공유, 지역별 Open Graph
- 수집 이력, 마지막 정상 데이터 보존, 일일 03:40 KST 데이터 동기화

전국 17개 지표·전국 순위·숨은 점수와 정책 배지는 아직 제공하지 않습니다. 8개 지역의 편집 선정과 실제 원천 데이터를 구분해 표시합니다. 빈 월별 지표 응답을 임의의 점수로 채우지 않습니다.

## 개발

Node.js 22+, Java 21, Docker가 필요합니다.

1. `infra/.env.example`을 `infra/.env`로 복사하고 DB 비밀번호를 설정합니다.
2. `docker compose --env-file infra/.env -f infra/compose.yaml up -d`
3. `backend/.env.example`을 `backend/.env`로 복사해 DB 설정, TourAPI 디코딩 키, 32자 이상 관리자 토큰을 설정합니다.
4. `bash scripts/backend-dev.sh`
5. `cd frontend && npm ci && npm run dev`

데이터는 최초 수집 후 표시됩니다. `POST /api/v1/admin/sync/catalog`에 `Authorization: Bearer <ADMIN_TOKEN>`을 전달합니다. 관리자 API는 브라우저 프록시에서 허용하지 않습니다.

## 검증

```sh
cd backend && ./gradlew test bootJar
cd ../frontend && npm run lint && npm test && npm run build -- --webpack
PLAYWRIGHT_BASE_URL=https://your-domain.example npm run test:e2e
```

백엔드 통합 테스트는 Testcontainers의 독립 MySQL을 사용합니다. E2E는 데이터가 수집된 서버에서 데스크톱·모바일의 검색/상세/저장을 확인합니다. 공개 서버 테스트에서 생성한 좋아요는 테스트 종료 시 취소합니다.

## EC2 운영 배포

`infra/compose.production.yaml`: MySQL + Spring Boot + Next.js + Caddy. 빌드는 로컬에서 완료하고 실행 이미지만 전송합니다. Caddy가 인증서를 발급·갱신하고 HTTP를 HTTPS로 연결합니다.

1. 전용 Amazon Linux 2023 EC2에서 `scripts/bootstrap-server.sh` 실행: Docker, Compose, 2GB 스왑.
2. `/opt/dasibom/.env.production`을 예제에 맞게 작성하고 권한을 `600`으로 설정합니다. 실제 키는 저장소에 넣지 않습니다.
3. `SSH_KEY=/path/to/key.pem SITE_HOST=your-domain.example bash scripts/deploy.sh ec2-user@host release-tag`
4. `scripts/sync-server.sh`를 서버에 복사해 실행하면 초기 수집을 시작합니다.

### 비용과 가용성

- 서울 `t3.micro`, gp3 8GiB, CPU credit standard. 앱/DB는 외부에 포트를 노출하지 않습니다.
- 80/443만 공개하고 SSH는 관리자의 현재 IP만 허용합니다.
- JVM/Node/MySQL 메모리 상한, 로그 파일당 5MB × 2개, 자동 재시작을 적용합니다.
- 1GB 서버는 심사용 소규모 동시 접속을 목표로 합니다. 스왑은 메모리 급증 완충용이며 RAM의 대체물이 아닙니다.
- 도메인 미보유 시 sslip.io의 IP 기반 호스트를 사용할 수 있습니다. 외부 DNS 서비스에 의존하며, EC2 중지/시작으로 공인 IP가 바뀌면 URL·인증서·Origin 설정을 함께 변경해야 합니다. 심사 기간에는 인스턴스를 계속 실행하세요.
- 데이터베이스는 Docker 볼륨에 보존됩니다. `docker compose down -v`는 데이터를 삭제하므로 사용하지 마세요. 현재 단일 서버 구성은 고가용성/외부 백업을 제공하지 않습니다.
- 무료 플랜의 크레딧/만료일은 AWS 결제 콘솔에서 확인하세요. 비용 예산 알림은 실제 사용을 차단하는 한도가 아닙니다.

### 점검/복구

```sh
cd /opt/dasibom
sudo docker compose --env-file .env.production -f compose.production.yaml ps
sudo docker compose --env-file .env.production -f compose.production.yaml logs --tail 80 backend frontend caddy
curl -f https://your-domain.example/api/health
```

이전 이미지 태그가 남아 있다면 `.env.production`의 `RELEASE`를 이전 값으로 바꾸고 `up -d --wait`로 되돌릴 수 있습니다. 정기 수집은 실패한 데이터셋의 마지막 정상 응답을 보존하며, 오래된 집중률을 최신 예측으로 표시하지 않습니다.
