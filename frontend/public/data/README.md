# 서비스 지역 코드 기준 시군구 경계

- 원본: https://github.com/vuski/admdongkor/blob/master/ver20251231/HangJeongDong_ver20251231.geojson
- 원천: 통계청 SGIS, 가공 vuski/admdongkor
- 라이선스: CC BY 4.0
- 경계 기준: 2025-12-31. 서비스가 제공하는 252개 지역 코드와 동일한 기준이며 현재 행정구역 구분을 뜻하지 않는다.
- 가공: mapshaper `-simplify 8% keep-shapes -clean -dissolve sgg copy-fields=sido,sidonm,sggnm -o format=geojson precision=0.0001`
- 검증일: 2026-09-24. 252개 지역 코드와 경계 코드 집합이 일치함을 검사한다.

2026-07 경계를 이전 서비스 코드와 직접 결합하면 31개 지역이 누락되어 기준을 맞췄다. 방문량 API의 일대일 변경 코드는 백엔드 RegionCodes에서 연결하며, 단순 매핑할 수 없는 통합/분할 지역의 결측 점수는 0점으로 만들지 않는다. 경계 원본을 서비스 DB의 지역 코드와 독립적으로 교체하지 않는다.
