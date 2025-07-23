# 💬 Commit Message Convention (커밋 메시지 컨벤션)

커밋 메시지는 [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) 규칙을 따른다.

## ✅ 기본 구조
```
<type>(<scope>): <subject>

<body>

<footer>
```
- type: 커밋의 종류를 나타낸다. (Commit Type 참고)
- scope (선택사항): 커밋이 영향을 미치는 범위를 나타낸다. (예: login, post, css)
  - 예: `feat(auth): 소셜 로그인 추가`, `fix(ui): 버튼 색상 대비 개선`, `docs(api): 엔드포인트 문서 추가`
- subject: 커밋에 대한 간결한 요약을 나타낸다.
  - 작성 규칙
    1. 50자 이내로 작성한다.
    2. 한글로 작성하되, 영어로 시작할 경우 대문자를 사용한다.
    3. 현재 시제, 명령형으로 작성한다. ("추가했다" ❌, "추가" ⭕)
    4. 마침표를 사용하지 않는다.
- body (선택사항): 긴 설명이 필요할 경우 작성한다.
  - 작성 규칙
    1.  무엇을, 왜 변경했는지 설명한다.
    2.  72자마다 줄바꿈한다.
    3.  제목과 본문 사이 빈 줄을 추가한다.
- footer (선택사항): 이슈 트래커 ID 등을 명시한다. (예: Closes #123)
  - 작성 규칙
    1. 이슈 번호 참조
    2. Breaking Changes 명시

### 예시
- 간단한 커밋
```bash
docs: README.md 파일에 프로젝트 실행 방법 추가
```
- 상세한 커밋
```bash
feat(login): 소셜 로그인 기능 추가

- 구글, 카카오 소셜 로그인 기능 구현
- 로그인 시 JWT 토큰 발급

Closes #45
```

## ✅ Commit Type

| 타입         | 설명                | 예시                             |
| ---------- | ----------------- | ------------------------------ |
| `feat`     | 새로운 기능 추가            | `feat: 감정일기 작성 기능 추가`        |
| `fix`      | 버그 수정             | `fix: 로그인 오류 수정`            |
| `docs`     | 문서 관련 수정          | `docs: README.md 수정`        |
| `style`    | 코드 포맷팅, 세미콜론 누락 등    | `style: 코드 스타일 정리`          |
| `refactor` | 리팩토링 (기능 변경 없음)   | `refactor: ♻useEffect 구조 개선` |
| `test`     | 테스트 추가 및 수정       | `test: 감정일기 테스트 추가`          |
| `chore`    | 빌드/패키지 설정 등 기타 수정 | `chore: ESLint 설정 추가`       |
| `design`   |  CSS 등 사용자 UI 디자인 변경    | `design: App.jsx CSS 수정 |
| `rename`   | 파일 또는 폴더명 변경/이동         | `rename: test.java -> index.java로 파일명 변경` |
| `remove`   | 파일 삭제                          | `remove: test.java 삭제` |
