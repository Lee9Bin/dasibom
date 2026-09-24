# API 활용 권한 점검과 추가 신청 여부

2026-09-24 현재 로컬에 설정된 TourAPI 키를 공식 `apis.data.go.kr/B551011`에 전달해 최소 건수로 조회했다. 키는 기록하지 않았다. 아래 성공은 개발/운영 등급이나 일일 허용량을 확인한 것은 아니며, 해당 호출에 접근할 수 있음을 뜻한다.

| 서비스 | 검증 호출 | 결과 | 추가 신청 |
| --- | --- | --- | --- |
| 관광사진갤러리 | PhotoGalleryService1/gallerySearchList1 | 구례군 사진 응답 성공 | 현재 사진 개선에 새 API 불필요 |
| 지역별 관광자원 수요 | AreaTarResDemService/areaTarSvcDemList | 지표 코드 지정 시 실제 값 제공 | 재신청 불필요. 파라미터·응답 필드 수정 |
| 관광지별 연관관광지 | TarRlteTarService1/areaBasedList1 | 정상 응답, 결과 1,000건 | 코스 추천 개발에 현재 키 활용 가능 |
| 반려동물 동반여행 | KorPetTourService2/areaBasedList2 | 정상 응답, 전체 9,676건 | 현재 키 활용 가능 |
| 생태관광 | GreenTourService1/areaBasedList1 | 정상 응답, 전체 5건 | 현재 키 활용 가능. 실제 대상·범위는 콘텐츠 검증 필요 |
| 웰니스 | WellnessTursmService/areaBasedList | langDivCd=KOR 포함 시 정상 응답, 전체 169건 | 현재 키 활용 가능 |

숫자는 점검 순간 API의 totalCount이며 지역별 서비스 품질이나 추천 가능한 장소 수를 뜻하지 않는다. 위 4개 확장 API의 조회 가능 여부를 확인했지만, 사용자 화면에 모두 연동한 것은 아니다.

## 이번에 발견한 기존 연동 문제

- 자원 수요 지표 코드/이름 필드는 `tarSvcDemIxCd`, `tarSvcDemIxNm`, `culResDemIxCd`, `culResDemIxNm`이다. 기존 코드는 Lcls/Mcls 필드를 읽어 유효한 항목을 버릴 수 있었다.
- 실서비스에서 지표 코드 파라미터를 생략하면 빈 응답이 돌아왔으며, `tarSvcDemIxCd=1112` 지정 시 값이 반환됐다. aggregate `11`은 서비스 수요 전체 1개 지표이며 12개 세부 지표를 뜻하지 않는다. 문화 aggregate `12`도 마찬가지다.
- 웰니스 `langDivCd`는 필수다. 누락 시 `NO_MANDATORY_REQUEST_PARAMETERS_ERROR1(langDivCd)`가 반환됐다. 권한 미승인으로 판단하면 안 된다.
- 방문량/현재 관광정보와 서비스 DB의 행정코드 기준이 다르다. 일대일 변환 가능한 광주·전남 27개 지역은 명시적인 매핑을 사용하고, 분할·통합된 인천 3개 구의 방문량은 추정 배분하지 않는다.

## 앞으로 신청을 검토할 때

우선 현재 승인된 API로 취향·경험·코스를 연결한다. 트래픽이 늘면 공공데이터포털 마이페이지에서 각각의 운영계정 승인 상태와 일일 호출 한도를 확인해 필요한 서비스만 운영계정/트래픽 증량을 신청한다. 현재 이번 버그 수정을 위해 추가로 신청해야 하는 API는 확인되지 않았다.

공식 확인 경로: https://www.data.go.kr/ 및 https://api.visitkorea.or.kr/ . 작업 폴더의 한국관광공사 활용매뉴얼과 실제 응답을 함께 대조했다.
