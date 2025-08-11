# BIF (Business Intelligence Framework) Backend

BIF 프로젝트의 백엔드 애플리케이션입니다.

## 기술 스택

- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Database**: H2 (개발용), MySQL (운영용)
- **Build Tool**: Gradle
- **AI Service**: Azure OpenAI
- **Content Moderation**: Azure OpenAI Moderation API

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

## 콘텐츠 위험도 검사 (Content Moderation)

Azure OpenAI Moderation API를 사용하여 사용자 입력과 AI 응답에 대한 위험도를 검사합니다.

### 설정

`application-dev.yml`에서 Moderation API 설정을 조정할 수 있습니다:

```yaml
azure:
  openai:
    moderation:
      enabled: true                    # Moderation API 사용 여부
      threshold: 0.7                  # 전체 위험도 임계값
      block-on-failure: true          # API 실패 시 차단 여부
      category-thresholds:            # 카테고리별 개별 임계값
        hate: 0.7                     # 혐오 표현
        hate-threatening: 0.6         # 혐오/위협
        self-harm: 0.5                # 자해
        sexual: 0.7                   # 성적 콘텐츠
        sexual-minors: 0.3            # 성적/미성년자 (더 엄격)
        violence: 0.7                 # 폭력
        violence-graphic: 0.6         # 폭력/그래픽
```

### 동작 방식

1. **사용자 입력 검사**: AI 응답 생성 전 사용자 입력의 위험도 검사
2. **AI 응답 검사**: AI가 생성한 응답의 위험도 검사
3. **자동 차단**: 위험도가 임계값을 초과하면 `ContentModerationException` 발생
4. **설정 기반 처리**: API 실패 시 설정에 따라 처리 방식 결정

### 사용 예제

```java
@Service
public class ExampleService {
    
    @Autowired
    private AzureOpenAiClient aiClient;
    
    public void generateResponse(String userInput) {
        try {
            // 내부적으로 moderation 체크 수행
            AiResponse response = aiClient.generate(new AiRequest(userInput));
            // 안전한 응답 처리
        } catch (ContentModerationException e) {
            // 위험한 콘텐츠 처리
            log.warn("위험한 콘텐츠 감지: {}", e.getModerationResult());
        }
    }
}
```

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