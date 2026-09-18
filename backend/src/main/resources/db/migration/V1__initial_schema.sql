CREATE TABLE regions (
    code VARCHAR(5) PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    area_code VARCHAR(2) NOT NULL,
    area_name VARCHAR(40) NOT NULL,
    tagline VARCHAR(160) NOT NULL,
    theme VARCHAR(24) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    anchor_place VARCHAR(100) NOT NULL
);

CREATE TABLE datasets (
    region_code VARCHAR(5) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    payload JSON NOT NULL,
    fetched_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (region_code, kind),
    FOREIGN KEY (region_code) REFERENCES regions(code)
);

CREATE TABLE region_likes (
    region_code VARCHAR(5) NOT NULL,
    visitor_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (region_code, visitor_id),
    FOREIGN KEY (region_code) REFERENCES regions(code)
);

CREATE TABLE sync_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    finished_at TIMESTAMP(6) NULL,
    status VARCHAR(24) NOT NULL,
    successful INT NOT NULL DEFAULT 0,
    failed INT NOT NULL DEFAULT 0,
    summary VARCHAR(2000),
    INDEX ix_sync_started(started_at)
);

INSERT INTO regions VALUES
('51130','원주시','51','강원특별자치도','숲과 예술 사이, 조금 느린 하루','CULTURE',37.3422,127.9202,'간현관광지'),
('51750','영월군','51','강원특별자치도','별빛이 머무는 강마을','NATURE',37.1837,128.4619,'청령포'),
('51770','정선군','51','강원특별자치도','굽이진 산길 끝에서 만나는 쉼','HEALING',37.3806,128.6609,'아우라지'),
('43800','단양군','43','충청북도','강을 따라 펼쳐지는 작은 모험','NATURE',36.9846,128.3655,'도담삼봉'),
('43150','제천시','43','충청북도','호수의 고요를 닮은 여행','HEALING',37.1326,128.1910,'의림지'),
('46730','구례군','46','전라남도','지리산 아래, 계절을 천천히 걷다','NATURE',35.2025,127.4626,'화엄사'),
('48840','남해군','48','경상남도','바다와 마을이 맞닿는 풍경','HEALING',34.8377,127.8924,'남해 독일마을'),
('48850','하동군','48','경상남도','차 한 잔과 섬진강의 여유','FOOD',35.0672,127.7513,'하동 최참판댁');

