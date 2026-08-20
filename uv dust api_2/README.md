# UV / 미세먼지 API 연동

한참 "SERVICE_KEY_IS_NOT_REGISTERED_ERROR"가 나서 키 동기화 지연인 줄 알았는데,
**에어코리아 기술문서의 에러코드표를 보니 에러코드 30번 설명이 이거였음:**

> 등록하지 않은 서비스키 — 잘못된 서비스키를 사용하였거나 **서비스키를 URL 인코딩하지 않음**

서비스키 안에 `+`, `=` 문자가 들어있는데, curl/브라우저에 그대로 넣거나
`UriComponentsBuilder...toUriString()`으로 문자열만 만들어서 RestTemplate에 넘기면
이 문자들이 인코딩 안 된 채로 나가서 서버가 키를 못 알아봄. (`+`는 공백으로,
`=`는 파라미터 구분자로 잘못 해석됨)

**고친 내용:** 모든 클라이언트에서 `.build().encode(StandardCharsets.UTF_8).toUri()`로
URI 객체를 만들고, `restTemplate.getForObject(URI, ...)` (String이 아니라 URI 오버로드)를
호출하도록 변경. curl로 테스트할 때도 Decoding 키를 그냥 넣지 말고, 아래처럼
퍼센트 인코딩된 값을 써야 함.

## 구조
```
client/
  KmaUvClient.java         기상청 자외선지수 (getUVIdxV5)
  AirKoreaDustClient.java  에어코리아 미세먼지 (getCtprvnRltmMesureDnsty, 시도별 조회)
  RegionResolver.java      "서울"+"강남구" -> areaNo 등 지역코드 자동 변환
  KakaoGeocodingClient.java  위경도(GPS) -> "서울특별시"+"강남구" 변환 (신규, 카카오 로컬 API)
  SidoNameConverter.java   "서울특별시" -> "서울" 같은 축약형 변환 (에어코리아 sidoName용, 신규)
  GridConverter.java       위경도 -> 기상청 격자좌표(nx, ny) 변환 (참고용, RegionResolver로 대체됨)
dto/
  EnvironmentInfo.java    두 API 응답을 통일한 공통 포맷 (type, value, level, region, observedAt)
config/
  EnvironmentApiConfig.java  RestTemplate + data.go.kr 서비스키 주입
exception/
  EnvironmentApiException.java   공통 예외
  EnvironmentExceptionHandler.java  502로 매핑
controller/
  EnvironmentController.java
    GET /api/environment/uv?sido=&gugun=
    GET /api/environment/dust?sido=&gugun=
    GET /api/environment/uv/by-location?lat=&lon=       (신규)
    GET /api/environment/dust/by-location?lat=&lon=     (신규)
    GET /api/location?lat=&lon=                         (신규, UV/미세먼지 없이 순수 행정구역 조회용)
resources/
  kma-area-codes.csv      기상청 지역코드 원본 엑셀(dfs-zone-tree)에서 구/군 단위 256개만 추출
```

## 위치 기반(GPS) 엔드포인트 추가됨

`sido`/`gugun` 이름을 직접 넘기는 기존 방식 외에, **위경도만 넘기면 자동으로 처리**하는 엔드포인트를 추가:

```
GET /api/environment/uv/by-location?lat=37.4979&lon=127.0276
GET /api/environment/dust/by-location?lat=37.4979&lon=127.0276
```

내부 흐름: `lat/lon` → (카카오 로컬 API `coord2regioncode`) → `region_1depth_name`(시도), `region_2depth_name`(구/군), `region_3depth_name`(동)
→ 기존 `RegionResolver`/`SidoNameConverter`로 areaNo·sidoName 변환 → 기존 로직 그대로 재사용.

**`KakaoGeocodingClient.GeoRegion`은 `sido`, `gugun`, `dong` 세 필드를 돌려줌.** UV/미세먼지 조회에는 `sido`,
`gugun`만 쓰지만, `dong`도 같이 가져와둬서 화면 표시용(예: "서울특별시 강남구 역삼동")으로 바로 쓸 수 있음.

순수하게 좌표 → 행정구역 이름만 확인하고 싶을 때(UV/미세먼지 호출 없이)는:

```
GET /api/location?lat=37.4979&lon=127.0276
```

이걸 호출하면 `{"sido":"...", "gugun":"...", "dong":"..."}` 형태로 바로 확인 가능. 카카오 API 연동이
제대로 됐는지 먼저 이걸로 테스트해보고, 되면 `/by-location` 엔드포인트들도 자동으로 됨.

**추가로 필요한 것: 카카오 REST API 키.** data.go.kr 키와는 완전히 별개.
[카카오 디벨로퍼스](https://developers.kakao.com)에서 애플리케이션 등록 후 REST API 키 발급받아서
`application.yml`에 `kakao.rest-api-key`로 넣어둠. (`application-example.yml` 참고).

프론트에서 브라우저/앱의 GPS로 위경도만 얻어서 이 엔드포인트로 넘기면 되고,
"강남구" 같은 이름을 프론트가 따로 알 필요는 없어짐.

## RegionResolver로 뭐가 바뀌었나

이전엔 프론트가 `areaNo`(기상청), `sidoName`+`districtKeyword`(에어코리아)를 각각 직접 넘겨야 했는데,
이제 **`sido`, `gugun` 두 개만 넘기면** 서버가 알아서 처리:

```
GET /api/environment/uv?sido=서울&gugun=강남구
GET /api/environment/dust?sido=서울&gugun=강남구
```

`RegionResolver`가 `kma-area-codes.csv`(전국 256개 구/군, 기상청이 배포한 dfs-zone-tree 엑셀에서 추출)에서
`sido`가 포함되고 `gugun`이 정확히 일치하는 행을 찾아서 `areaNo`를 돌려줌. 목록에 없는 이름이면
`EnvironmentApiException`을 던지고 502로 응답.

에어코리아 `sidoName`은 축약형("서울")만 받기 때문에, `RegionResolver`가 돌려주는 정식명("서울특별시")을
`SidoNameConverter.toShort()`로 변환해서 넘김.

## 미세먼지 등급 값

`pm10Grade`/`pm25Grade` API 응답 필드(1~4)를 그대로 써서 좋음/보통/나쁨/매우나쁨으로 변환하도록 수정
필드가 빈 값으로 오는 경우엔 기존 계산 로직(`resolvePm10Level`/`resolvePm25Level`)으로 폴백.

## 실행 전 해야 할 것

1. `pom.xml`(or build.gradle)에 `spring-boot-starter-web` 있는지 확인
2. 공공데이터포털에서 두 API 각각 활용신청 → 서비스키 발급
   - 기상청_생활기상지수 조회서비스(3.0) - getUVIdxV5
   - 한국환경공단_에어코리아_대기오염정보 - getCtprvnRltmMesureDnsty (시도별 실시간 측정정보 조회)
3. `application-example.yml` 참고해서 로컬에 `application.yml` 만들고 `DATA_GO_KR_SERVICE_KEY`, `KAKAO_REST_API_KEY` 환경변수로 주입. `.gitignore`에 이미 포함해뒀음.
4. 카카오 디벨로퍼스에서 애플리케이션 등록 + REST API 키 발급 (위치 기반 엔드포인트 쓰려면 필수)

## 아직 안 끝난 부분

- UV API의 `h0` 필드만 씀 — 시간대별(h3, h6...h75) 데이터도 필요하면 파싱 확장
- **UV BASE_URL 재확인 권장**: `LivingWthrIdxServiceV5/getUVIdxV5`는 V5 네이밍 규칙을 따른 추정값. 활용신청 상세페이지 실제 요청주소와 다르면 교체
- DAILY_UV_STATUS 테이블 저장 배치/스케줄러는 별도 구현 필요
- 카카오 API 응답에서 region_type "B"(법정동)를 우선 쓰도록 했는데, 실제 응답 구조가 문서와 다르면(예: B 타입이 없는 특수 지역) 폴백 로직(`documents.get(0)`) 확인 필요

## curl 테스트 (제대로 인코딩해서)

Decoding 키를 curl에 그대로 넣으면 위에서 설명한 버그가 재현됨. 아래처럼
**퍼센트 인코딩된 값**(공공데이터포털의 "Encoding" 키, 또는 Decoding 키를 직접 URL 인코딩한 값)을 써야 함:

```bash
# 자외선 (서비스키의 + -> %2B, = -> %3D 로 미리 인코딩해서 넣기)
curl "https://apis.data.go.kr/1360000/LivingWthrIdxServiceV5/getUVIdxV5?serviceKey=인코딩된키&pageNo=1&numOfRows=10&dataType=JSON&areaNo=1168000000&time=yyyyMMddHH중_최근3시간단위"

# 미세먼지 (시도별)
curl "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty?serviceKey=인코딩된키&returnType=json&numOfRows=100&pageNo=1&sidoName=서울&ver=1.0"
```

`resultCode`가 `00`이면 성공.

## 위치 기반 엔드포인트 curl 테스트 (스프링부트 실행 후)

data.go.kr을 직접 부르는 게 아니라 본인 서버(예: localhost:8080)를 호출하는 거라 훨씬 간단함:

```bash
curl "http://localhost:8080/api/location?lat=37.4979&lon=127.0276"
curl "http://localhost:8080/api/environment/uv/by-location?lat=37.4979&lon=127.0276"
curl "http://localhost:8080/api/environment/dust/by-location?lat=37.4979&lon=127.0276"
```

(위 좌표는 강남역 근처 예시. 카카오 REST API 키가 `application.yml`에 제대로 들어있어야 동작함)

## areaNo 참고 (법정동코드 기준, kma-area-codes.csv로 자동화됨)

| 지역 | areaNo | 격자 X,Y |
|---|---|---|
| 서울 전체 | 1100000000 | 60, 127 |
| 서울 강남구 | 1168000000 | 61, 126 |

CSV에 전국 256개 구/군이 다 들어있어서 이제 이 표를 직접 찾아볼 필요 없이 `RegionResolver`가 처리함.
