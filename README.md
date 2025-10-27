# 🧶 Knitly - Online Knitting Pattern & Community Platform

> 취미로 뜨개질을 즐기는 사람들이 자유롭게 도안을 제작하고 판매하며,
> 서로의 창작물을 공유할 수 있는 커뮤니티 기반 플랫폼입니다.
> 단순한 도안 판매를 넘어, 제작자와 소비자가 함께 성장하는 창작 생태계를 지향합니다.
---
## ✨ 프로젝트 개요

* **프로젝트명:** Knitly
* **주제:** 뜨개질 도안 판매 및 커뮤니티 서비스
* **주제 선정 이유:**
  “취미로 뜨개질을 하는 사람들이 자유롭게 도안을 제작하고 공유할 수 있는 플랫폼이 있으면 좋겠다는 아이디어에서 출발하였습니다.”
* **개발 목표:**

  * 캐싱과 락을 통한 **성능 향상 및 데이터 일관성 확보**
  * 서비스 간 **데이터 흐름 및 비동기 처리 구현**
  * **Redis 기반**의 인기순 조회, 동시성 제어, 찜 기능 관리
  * **외부 API 연동** (토스페이먼츠, OAuth 2.0, PDF 변환 등)
  * **예외 처리 강화 및 테스트 코드 검증**
  * 전체 서비스의 **안정성 및 유지보수성 확보**

---

## 💻 기술적 특징

| 분류                 | 기술 스택                                                   |
| ------------------ | ------------------------------------------------------- |
| **Backend**        | Spring Boot 3.5.x, Java 17, JPA(Hibernate), MySQL       |
| **Infra**          | AWS EC2, Docker, Redis, Nginx                           |
| **API**            | OAuth 2.0 (Google Login), Toss Payments API, Swagger UI |
| **DevOps**         | GitHub Actions (CI/CD), Docker Compose                  |
| **Test & Monitor** | JUnit5, MockMvc, Prometheus, Grafana (검토 중)             |
| **Tooling**        | IntelliJ, Postman, DBeaver, Slack, Notion               |

<img width="500" height="600" alt="아키텍처 drawio" src="https://github.com/user-attachments/assets/15f29ed1-f7f1-4ad3-8b0f-ef4a361c8959" />

---

## 🧩 주요 기능 요약

| 구분             | 기능                                           |
| -------------- | -------------------------------------------- |
| **회원가입 / 로그인** | 구글 OAuth 2.0 로그인, JWT 기반 인증/인가               |
| **도안 제작**      | 10x10 격자(Grid) 기반 UI에서 도안 제작 → PDF 변환 저장     |
| **상품 판매**      | 제작한 도안 또는 PDF 업로드 / 무료·한정 판매 지원              |
| **상품 구매**      | Queue 기반 이메일 자동 발송 / Redis 락으로 재고 관리         |
| **상품 조회**      | Redis ZSet으로 인기순 정렬 / 카테고리별·가격순·최신순          |
| **찜 및 리뷰**     | Redis 찜 카운트 / Rabbit Queue 기반 DB 동기화 / 리뷰 작성 |
| **커뮤니티**       | 게시글 및 댓글 CRUD / 소프트 딜리트 기반 관리                |
| **마이페이지**      | 구매내역, 찜목록, 이벤트 참여내역, 구독 관리                   |
| **결제**         | Toss Payments API 연동 / 모의결제 지원               |
| **인증/인가**      | JWT 기반 인증 필터 / 조회 외 모든 API 토큰 검증             |
---

# 팀원
|[김예진](https://github.com/dpwls8984)|[김시현](https://github.com/SiHejt)|[나웅철](https://github.com/No-366)|[부종우](https://github.com/Boojw)|[정혜연](https://github.com/hznnoy)|
|:-:|:-:|:-:|:-:|:-:|
|<img width="150" height="150" alt="image" src="https://github.com/user-attachments/assets/7686f2dd-8ca1-47d5-be94-79ac13d508b4" />|<img width="150" height="150" alt="image" src="https://github.com/user-attachments/assets/0ed8ffdd-5a65-4e91-9ec0-568d8afad469" />|<img width="150" height="150" alt="image" src="https://github.com/user-attachments/assets/958906b3-1bdd-4546-bf1a-4932d7660bfd" />|<img width="150" height="420" alt="150" src="https://github.com/user-attachments/assets/d37491fa-1b9e-4754-896e-bb3c5794b665" />|<img width="150" height="150" alt="image" src="https://github.com/user-attachments/assets/061156b1-634c-416a-b32d-d8b9e9cd9de6" />|
|BE, FE|BE, FE|BE, FE|BE, FE|BE, FE|



## 🛠️ 역할 분담

| 이름   | 담당 기능 |
|--------|-----------|
| **김예진** | - 상품 구매(Redis, 분산 락)<br>- 상품 판매 |
| **김시현** | - 상품 리뷰<br>- 상품 찜(Redis & RabbitMQ) |
| **나웅철** | - Google 소셜 로그인<br>- JWT 인증<br>- 판매자 페이지 |
| **부종우** | - 커뮤니티 글<br>- 커뮤니티 댓글<br>- 마이페이지 조회(작성 글/댓글) |
| **정혜연** | - 도안 생성 + PDF 저장<br>- 도안 조회<br>- 상품 조회(Redis ZSet)<br>- 상품 결제(토스 페이먼츠 연동)|
---

# 🧩 기능 정의서 (Feature Definition)

## 🧑‍💻 회원가입 / 로그인

- **소셜 로그인 지원**
    - 구글 OAuth 2.0 기반 로그인
    - OAuth 인증 완료 시 프로젝트 내로 리다이렉트
- **회원 정보 수정**
    - 닉네임 / 프로필 이미지 수정 기능 ❌ (비활성)
- **회원 탈퇴 / 재가입**
    - 회원정보 테이블에 `status` 칼럼 추가하여 상태 관리
    - 탈퇴 시 소셜 계정 연결 끊기 로직 포함

---

## 🎨 도안 제작

- 10X10 격자에 기호를 삽입해 도안을 만들고 pdf 파일로 저장
- **프론트엔드**
    - 격자(Grid) UI 기반 도안 제작 화면
    - 사용자의 입력을 `gridData(JSON)` 형태로 백엔드에 전송
- **백엔드**
    - 수신한 `gridData`를 기반으로 PDF 파일 생성 및 저장

---

## 🛍️ 상품 판매

- **판매 대상**
    - 사용자가 직접 제작한 도안 또는 일반 PDF 업로드 가능
- **상품 형태**
    - 유료/무료 도안 + 상시/한정 수량 선택해 도안 상품 판매 지원
- **카테고리 분류**
    - 상의 / 하의 / 아우터 / 가방 / 기타 / 무료 / 한정

---

## 💸 상품 구매

- **구매 프로세스**
    - 사용자는 도안을 구매하면 이메일로 PDF 자동 발송
    - 이메일은 소셜 로그인 계정으로 발송됨
- **비동기 처리**
    - Kafka 대신 **Queue 구조** 사용
    - 큐에 `(사용자 계정, 도안 PDF)` 정보 저장 후 자동 이메일 전송
- **재고 관리**
    - 한정 수량 도안은 **Redis Lock + Queue** 기반 동시성 제어

---

## 🔍 상품 조회

- **정렬 기준**
    - 인기순 (Redis ZSet으로 구매수 기반 정렬)
    - 최신순, 가격순 정렬 지원
    - 메인페이지에서 인기 top5 상품 조회
- **페이징 처리**
    - Spring 서버사이드 렌더링 기반 `Paging` 처리
- **필터링**
    - 카테고리별 / 무료 / 한정 도안 조회 가능

---

## 🤎 **상품 찜 & 리뷰**

- Redis를 이용한 실시간 찜 카운트
- Rabbit 큐를 활용하여 10분마다 DB에 동기화
- 상품별 리뷰 작성 및 확인 가능

---

## 💬 커뮤니티

- **게시판**
    - 단일 게시판 구조
- **게시글 기능**
    - 글 등록 / 수정 / 삭제
- **댓글 기능**
    - 댓글 등록 / 삭제

---

## 🙋 마이페이지

- 찜, 구매내역, 이벤트 참여내역, 구독 관리
- 마이페이지 작성글/댓글 확인

---

## 🧵 판매자 개인 페이지

- **판매자 전용 페이지**
    - 판매중인 도안 목록 및 환영 문구 표시
- **구독 연결**
    - 판매자와 구독 기능 연계 (판매자별 구독 관리)

---

## 🎁 선착순 이벤트 (선택사항)

- **이벤트 생성**
    - 운영진이 직접 생성 (한정수량 / 무료 등)
- **재고 관리**
    - 한정수량 이벤트는 Redis Lock 사용
    - 필요 시 Queue 병행 사용

---

## 📬 구독 기능 (선택사항)

- 판매자를 구독한 사용자는 한 달간 해당 판매자의 모든 도안 다운로드 가능

---

## 💳 결제 기능

- 토스 페이먼츠 API와 연동하여 결제 가능

---

## 🔐 인증 / 인가

- **JWT 기반 인증 적용**
    - 조회(READ) API를 제외한 모든 Controller에 토큰 검증 로직 필수

---

✅ **정리 요약**

- 기술 키워드: `Spring Boot`, `Redis`, `JWT`, `Queue`, `H2`, `MySQL`, `Swagger`, `OAuth 2.0`, `Docker`
- 주요 비동기 처리: **Queue (Kafka 대체)**
- 주요 동시성 제어: **Redis Lock (Lettuce 기반)**
- 데이터 전달 형식: **JSON (gridData, API 응답)**

```mermaid
flowchart LR
  A[비회원]
  B1[소셜 로그인 - 구글 OAuth 2.0]
  B2[회원가입]
  B3[탈퇴 및 재가입]

  A --> B1
  B1 --> B2
  B1 --> B3

  D[도안 제작]
  D3[PDF 파일 생성·저장]

  B2 --> D
  D --> D3

  E[상품]
  E1[상품 판매 - 유료·무료 / 상시·한정]
  E2[상품 구매 - 이메일 발송 / 재고관리]
  E3[상품 조회 - 정렬]
  E4[상품 찜·리뷰]

  B2 --> E
  E --> E1
  E --> E2
  E --> E3
  E --> E4
  
  F[커뮤니티]
  F1[글 작성/댓글 작성]

	B2 --> F
	F --> F1

  G[마이페이지]
  G1[구매내역 확인]
  G2[리뷰/찜 확인]
  G3[판매자 스토어]
  G4[내 도안 목록]

  B2 --> G
  G --> G1
  G --> G2
  G --> G3
  G --> G4
  
  K[결제 기능]
  K1[토스 페이먼츠 API 연동]
  K2[결제 완료 후 이메일 발송]

  B2 --> K
  K --> K1
  K1 --> K2
```
---

## 📃 커밋 컨벤션 & 협업 규칙
### GitHub Flow(main/feature + develop)
> 이슈 생성 → 브랜치 생성 → 구현 → Commit & Push → PR 생성 → 코드 리뷰 → develop에 Merge

- `main`: 배포용 안정 브랜치
- `dev`: 기능 통합 브랜치
- `feature/{domain}`: 기능 단위 작업 브랜치
- `hotfix`: 오류 해결 브랜치
- `publishing`: AWS 배포용 브랜치
  
### 커밋 컨벤션

|유형 | 설명|
|---|---|
|init|초기설정|
|feat| 새로운 기능|
|fix| 버그 수정|
|docs|문서 변경(README 등)|
|style| 포맷/스타일(기능 변경 없음)|
|refactor| 리팩토링(동작 변경 없음)|
|test| 테스트|
|chore| 빌드/설정/의존성|
|remove| 파일/폴더 삭제|
|rename| 파일/폴더명 변경|

### 커밋 고유 번호
- 소셜로그인 100
- 커뮤니티 200
- 상품 300
    - 주문 301
    - 판매 302
    - 리뷰 303
    - 찜 등록/취소 304
    - 목록 조회 305
    - 결제 306
- 마이페이지 400
    - 판매자 페이지 401
    - 조회 402
    - 찜 조회 403
- 도안 500
    - 생성 501
    - 조회 / 삭제 502
- 이벤트 600

