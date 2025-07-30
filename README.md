# BIF (Business Intelligence Framework) Backend

BIF 프로젝트의 백엔드 애플리케이션입니다.

## 기술 스택

- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Database**: H2 (개발용), MySQL (운영용)
- **Build Tool**: Gradle
- **AI Service**: Azure OpenAI

## 프로젝트 구조

```
src/main/java/com/sage/bif/
├── common/          # 공통 컴포넌트
├── config/          # 설정 클래스
├── user/            # 사용자 관리
├── diary/           # 일기 관리
├── todo/            # 할 일 관리
├── simulation/      # 시뮬레이션
└── stats/           # 통계
```

## 환경 설정

### 🚀 빠른 시작

#### Windows 사용자
```bash
# 1. 환경 변수 설정 (처음 한 번만)
set-env.bat

# 2. 애플리케이션 실행
./gradlew bootRun
```

#### Linux/Mac 사용자
```bash
# 1. 환경 변수 설정
export AZURE_OPENAI_API_KEY=your-api-key
export AZURE_OPENAI_REGION=koreacentral
export AZURE_OPENAI_RESOURCE_NAME=oai-aiforbifproject
export AZURE_OPENAI_DEPLOYMENT_NAME=gpt-4.1-mini

# 2. 애플리케이션 실행
./gradlew bootRun
```

### 📖 상세 설정 가이드

**처음 설정하는 경우**: [환경 설정 가이드](./ENVIRONMENT_SETUP_GUIDE.md)를 참조하세요.

### 🔒 보안 설정

⚠️ **중요**: API 키와 같은 민감한 정보는 절대 Git에 커밋하지 마세요!

#### 환경 변수 관리
- `set-env.bat`: Windows 환경 변수 설정 스크립트
- `set-env-template.bat`: 템플릿 파일 (본인 정보로 수정 후 사용)
- 환경 변수를 통해 API 키 등 민감한 정보 관리

#### 설정 파일
- `ENVIRONMENT_SETUP_GUIDE.md`: 상세한 환경 설정 가이드
- `set-env-template.bat`: 템플릿 파일


## API 문서

애플리케이션 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs

## 데이터베이스

### 개발 환경
- **H2 Console**: http://localhost:8080/api/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (비어있음)

### 운영 환경
MySQL 데이터베이스 사용 (설정 필요)

## 로그

- **로그 파일**: `logs/bif-dev.log`
- **로그 레벨**: DEBUG (개발용)

## 개발 도구

- **Spring DevTools**: 활성화됨 (자동 재시작)
- **LiveReload**: 활성화됨