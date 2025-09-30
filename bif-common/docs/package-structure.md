# 프로젝트 패키지 구조 문서화

## 1. 개요

본 문서는 `bif` 프로젝트의 백엔드 애플리케이션 패키지 구조를 정의합니다. 본 프로젝트는 **모듈러 모놀리식(Modular Monolith)** 아키텍처 패턴을 따르며, 각 비즈니스 도메인(User, Todo, Diary 등)이 독립적인 모듈로 구성됩니다. 

## 2. 전체 패키지 구조

`com.sage.bif`을 루트 패키지로 하며, 주요 모듈과 공통 기능들이 계층적으로 구성됩니다.
```
com.sage.bif/
├── user/                      # User 도메인 모듈 (사용자 관리)
│   ├── controller/            # REST API 엔드포인트 정의
│   │   └── UserController.java
│   ├── service/               # 비즈니스 로직 처리
│   │   ├── UserService.java
│   │   └── UserServiceImpl.java
│   ├── repository/            # 데이터베이스 접근 (JPA Repository)
│   │   └── UserRepository.java
│   ├── entity/                # JPA 엔티티 정의 (데이터베이스 테이블 매핑)
│   │   └── User.java
│   ├── dto/                   # 데이터 전송 객체 (Request/Response DTO)
│   │   ├── request/
│   │   │   └── UserCreateRequest.java
│   │   └── response/
│   │       └── UserResponse.java
│   ├── event/                 # Spring Event 관련 (도메인 이벤트 발행/수신)
│   │   ├── model/             # 도메인 이벤트 객체 정의
│   │   │   └── UserRegisteredEvent.java
│   │   └── listener/          # 도메인 이벤트 리스너 정의
│   │       └── UserRegisteredListener.java
│   └── exception/             # 도메인 관련 커스텀 예외 정의
│       └── UserNotFoundException.java
│
├── todo/                      # Todo 도메인 모듈 (User 도메인과 동일한 패턴)
│   ├── controller/
│   ├── service/
│   │   ├── TodoService.java
│   │   └── TodoServiceImpl.java
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── event/
│   │   ├── model/
│   │   │   └── TodoCompletedEvent.java
│   │   └── listener/
│   │       └── TodoCompletedListener.java
│   └── exception/
│
├── diary/                     # Diary 도메인 모듈 (User 도메인과 동일한 패턴)
│   ├── controller/
│   ├── service/
│   │   ├── DiaryService.java
│   │   └── DiaryServiceImpl.java
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── event/
│   │   ├── model/
│   │   │   └── DiaryFeedbackRequestedEvent.java
│   │   └── listener/
│   │       └── DiaryFeedbackListener.java
│   └── exception/
│
├── simulation/                # Simulation 도메인 모듈 (User 도메인과 동일한 패턴)
│   ├── controller/
│   ├── service/
│   │   ├── SimulationService.java
│   │   └── SimulationServiceImpl.java
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── event/
│   │   ├── model/
│   │   │   └── SimulationStepCompletedEvent.java
│   │   └── listener/
│   │       └── SimulationStepCompletedListener.java
│   └── exception/
│
├── stats/                     # Stats 도메인 모듈 (User 도메인과 동일한 패턴)
│   ├── controller/
│   ├── service/
│   │   ├── StatsService.java
│   │   └── StatsServiceImpl.java
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── event/
│   │   ├── model/
│   │   │   └── StatsUpdatedEvent.java
│   │   └── listener/
│   │       └── StatsUpdatedListener.java
│   └── exception/
│
├── config/                   # 공통 설정 및 빈 정의
│   ├── SecurityConfig.java   # Spring Security 설정
│   └── JpaConfig.java        # JPA 관련 추가 설정 (선택 사항)
│
└── common/                   # 공통 모듈 (재사용 가능한 기능)
    ├── client/               # 외부 클라이언트 연동 (AI 클라이언트 등)
    │   └── ai/               # AI 서비스 클라이언트 관련
    │       ├── AiModelClient.java      # AI 클라이언트 인터페이스
    │       └── AzureOpenAiClient.java  # Azure OpenAI 구현체
    ├── constants/            # 애플리케이션 전역 상수 정의
    │   ├── ApiConstants.java
    │   ├── ErrorMessages.java
    │   └── aiclient/
    │       └── AiClientConfig.java
    ├── dto/                  # 공통 DTO (예: API 응답 Wrapper)
    │   └── ApiResponse.java
    ├── event/                # 공통 이벤트 및 이벤트 리스너
    │   ├── model/            # 공통/기반 이벤트 객체 (예: BaseEvent)
    │   │   └── BaseEvent.java
    │   └── listener/         # 애플리케이션 전체 이벤트 리스너
    │       └── ApplicationEventListener.java
    ├── exception/            # 전역 예외 처리 및 에러 코드 정의
    │   ├── BaseException.java
    │   └── ErrorCode.java
    └── util/                 # 공통 유틸리티 클래스
        ├── DateUtils.java
        ├── JsonUtils.java
        └── ValidationUtils.java
```

## 3. 패키지별 상세 설명

### 3.1. 도메인 모듈 (`user/`, `todo/`, `diary/`, `simulation/`, `stats/`)

각 도메인 모듈은 독립적인 비즈니스 영역을 담당하며, 다음과 같은 내부 구조를 가집니다.

* **`controller/`**:
    * RESTful API 엔드포인트를 정의합니다. 클라이언트의 요청을 받아 서비스 계층으로 전달하고, 서비스 계층의 결과를 클라이언트에 응답합니다. `RestController` 어노테이션 사용.
* **`service/`**:
    * 해당 도메인의 핵심 비즈니스 로직을 처리합니다. `@Transactional` 어노테이션을 통해 트랜잭션을 관리하고, Repository를 통해 데이터를 조작합니다.
    * 인터페이스(`UserService`)와 구현체(`DefaultUserService`) 분리하여 의존성 역전 원칙(DIP)을 따릅니다.
* **`repository/`**:
    * 데이터베이스 접근 로직을 담당합니다. Spring Data JPA의 `JpaRepository` 인터페이스를 확장하여 사용합니다.
* **`entity/`**:
    * JPA 엔티티 클래스들을 정의합니다. 데이터베이스 테이블과 1:1로 매핑되는 객체들입니다.
* **`dto/`**:
    * `request/`: 클라이언트로부터 요청받는 데이터를 매핑하는 DTO (Data Transfer Object)입니다.
    * `response/`: 클라이언트에 응답으로 전달하는 데이터를 매핑하는 DTO입니다.
* **`event/`**:
    * **`model/`**: 해당 도메인에서 발생하는 이벤트(예: `UserRegisteredEvent`)를 정의하는 POJO(Plain Old Java Object) 클래스들입니다.
    * **`listener/`**: `model`에 정의된 이벤트를 수신하여 특정 로직을 수행하는 이벤트 리스너 클래스들입니다. `@EventListener` 또는 `@TransactionalEventListener`를 사용합니다.
* **`exception/`**:
    * 해당 도메인에 특화된 커스텀 예외 클래스들을 정의합니다 (예: `UserNotFoundException`).

### 3.2. 공통 모듈 (`config/`, `common/`)

애플리케이션 전체에서 공통적으로 사용되거나, 특정 도메인에 종속되지 않는 기능들을 모아둡니다.

* **`config/`**:
    * 애플리케이션의 전반적인 설정을 정의하는 클래스들입니다.
    * `SecurityConfig.java`: Spring Security를 위한 인증/인가 및 필터 체인 설정.
    * `JpaConfig.java`: JPA 관련 추가 설정 (예: Auditing, Datasource 설정).
    * `SwaggerConfig.java`: Springdoc OpenAPI (Swagger UI) 문서화를 위한 설정.
* **`common/exception/`**:
    * **`BaseException.java`**: 모든 커스텀 예외가 상속받는 기본 예외 클래스입니다.
    * **`ErrorCode.java`**: 애플리케이션 전반에서 사용되는 오류 코드 및 관련 메시지, HTTP 상태 코드를 정의하는 Enum 클래스입니다.
    * `GlobalExceptionHandler` (컨트롤러 어드바이스): `@RestControllerAdvice`를 사용하여 모든 컨트롤러에서 발생하는 예외를 일관되게 처리하고 `ApiResponse` 형식으로 응답합니다. (이 파일은 일반적으로 `config/` 또는 `common/exception/` 아래에 위치할 수 있습니다. 위 예시에서는 `config`에 포함될 것으로 예상.)
* **`common/event/`**:
    * **`model/`**: 모든 도메인 이벤트의 기반이 될 수 있는 `BaseEvent`와 같은 공통 이벤트 객체를 정의합니다.
    * **`listener/`**: 애플리케이션 전반에 걸쳐 공통적으로 이벤트를 처리하는 리스너를 정의할 수 있습니다.
* **`common/util/`**:
    * 날짜 처리, JSON 파싱, 유효성 검사 등 애플리케이션 전반에서 재사용 가능한 유틸리티 메서드를 제공하는 클래스들입니다.
* **`common/constants/`**:
    * API 엔드포인트 상수, 에러 메시지 상수, AI 클라이언트 관련 설정 상수 등 애플리케이션 전반에서 사용되는 고정 값들을 정의합니다.
    * `aiclient/AiClientConfig.java`: AI 클라이언트의 설정과 관련된 상수 또는 설정 클래스.
* **`common/dto/`**:
    * **`ApiResponse.java`**: 모든 API 응답을 감싸는 공통 래퍼 클래스입니다. 성공/실패 여부, 메시지, 실제 데이터를 일관된 형식으로 제공합니다.
* **`common/client/ai/`**:
    * 외부 AI 서비스(예: Azure OpenAI Service)와 통신하기 위한 클라이언트 관련 모듈입니다.
    * **`AiModelClient.java`**: AI 모델과의 상호작용을 추상화한 인터페이스입니다. 서비스 계층은 이 인터페이스에 의존합니다.
    * **`AzureOpenAiClient.java`**: `AiModelClient` 인터페이스의 구현체로, Azure OpenAI Service의 Java SDK를 사용하여 실제 AI API를 호출하는 로직을 포함합니다.
