# ♟️ Git 브랜치 전략

- `GitHub Flow`를 기반으로 하되 `develop` 브랜치를 추가한 전략을 사용한다.

```
main       ───────────▶ 🚀 운영(배포) 브랜치
  │
  └──▶ dev             🌱 통합 개발 브랜치
        ├──▶ feature/* 💡 새로운 기능 개발
        ├──▶ fix/*     🛠️ 버그 수정
        ├──▶ docs/*    📚 문서 수정
        └──▶ hotfix/*  🔥 운영 중 긴급 수정
```

## 🧩 Branch 종류

| 브랜치명        | 용도            | 예시                                   |
| ----------- | ------------- | ------------------------------------ |
| `main`      | 최종 배포 브랜치     | -                                    |
| `dev`       | 개발 통합 브랜치     | -                                    |
| `feature/{기능이름}` | 기능 개발 (신규)    | `feature/login-api`, `feature/diary` |
| `fix/{버그명}`     | 기능 수정 (버그 포함) | `fix/diary-error`, `fix/login-bug`   |
| `hotfix/{긴급수정명}`  | 운영 중 긴급 수정    | `hotfix/critical-error`              |
| `docs/{문서명}`    | 문서 관련 수정      | `docs/readme-update`, `docs/api-documentation`     |

- `main`: 오직 `dev` 브랜치와 `hotfix` 브랜치의 내용만 Merge된다. PR(Pull Request)을 통해서만 병합한다. 자동 배포 대상.
- `dev`: 개발 중인 기능들이 통합되는 브랜치. 모든 기능 개발은 이 브랜치에서 시작된다. 주기적으로 `main`으로 Merge한다.
- `feature/{기능이름}`: 새로운 기능 개발을 위한 브랜치.
- `fix/{버그명}`: 버그 수정을 위한 브랜치.
- `hotfix/{긴급수정명}`: main 브랜치에 배포된 후 발생한 긴급 버그를 수정하기 위한 브랜치.
- `docs/{문서명}`: 문서 작성 관련 브랜치.

## 🔁 Workflow 흐름

### 🔥 기능 개발
1. 이슈 생성: GitHub Issues에서 작업 내용 정의
2. 브랜치 생성: `dev`에서 새 브랜치 생성
```bash
  git checkout dev
  git pull origin dev
  git checkout -b feature/{기능이름}
```
3. 개발 진행: 로컬에서 개발 및 커밋
```bash
  git add .
  git commit -m "feat: {커밋내역}"
```
4. 푸시 및 PR: 원격 저장소에 푸시 후 PR 생성
```bash
  git push origin feature/{기능이름}
```
5. 코드 리뷰: 최소 2명 이상의 팀원 리뷰
6. 병합: 리뷰 완료 후 `dev`로 병합
7. 브랜치 삭제: 병합 완료된 브랜치 삭제

### 🧪 릴리즈 (배포)
1. 병합: `dev` 브랜치의 코드를 `main` 브랜치로 병합
2. 푸시 및 버전 명시: `main` 브랜치에 푸시하고, 태그(Tag)를 생성하여 버전을 명시 (예: v1.0.0)

### 🐛 긴급 버그 수정
1. 이슈 생성: GitHub Issues에서 작업 내용 정의
2. 브랜치 생성: `main`에서 새 브랜치 생성
```bash
  git checkout main
  git pull origin main
  git checkout -b hotfix/{긴급수정명}
```
3. 개발 진행: 로컬에서 개발 및 커밋
```bash
  git add .
  git commit -m "fix: {커밋내역}"
```
4. 푸시 및 PR: 원격 저장소에 푸시 후 PR 생성
```bash
  git push origin hotfix/{긴급수정명}
```
5. 코드 리뷰: 최소 2명 이상의 팀원 리뷰
6. 병합: 작업이 끝난 `hotfix` 브랜치는 `dev` 브랜치에도 반드시 병합하여 코드 일관성을 유지
7. 브랜치 삭제: 병합 완료된 브랜치 삭제
