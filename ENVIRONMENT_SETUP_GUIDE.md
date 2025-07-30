# BIF 프로젝트 환경 설정 가이드

## 📋 개요

BIF 프로젝트를 실행하기 위해서는 Azure OpenAI API 키와 관련 설정이 필요합니다. 이 가이드는 `set-env.bat` 파일을 사용하여 환경 변수를 설정하는 방법을 설명합니다.

## 🚀 빠른 시작

### 1단계: Azure OpenAI 서비스 준비

1. **Azure Portal 접속**
   - https://portal.azure.com 에 접속
   - Azure 계정으로 로그인

2. **OpenAI 서비스 생성**
   - "Azure OpenAI" 검색
   - "Create" 클릭
   - 다음 정보 입력:
     - **Subscription**: 사용 가능한 구독 선택
     - **Resource group**: 새로 생성하거나 기존 그룹 선택
     - **Region**: `Korea Central` (권장)
     - **Name**: `oai-aiforbifproject` (또는 원하는 이름)
     - **Pricing tier**: `Standard S0` (무료 티어)

3. **API 키 및 엔드포인트 확인**
   - 생성된 리소스로 이동
   - 왼쪽 메뉴에서 "Keys and Endpoint" 클릭
   - **Key 1** 또는 **Key 2** 복사 (API 키)
   - **Endpoint** URL 복사

### 2단계: set-env.bat 파일 수정

1. **프로젝트 루트 디렉토리에서 `set-env.bat` 파일 열기**

2. **다음 변수들을 본인의 Azure OpenAI 정보로 수정**:

```batch
REM Azure OpenAI API 설정 (일시적 - 현재 세션)
set AZURE_OPENAI_API_KEY=YOUR_ACTUAL_API_KEY_HERE
set AZURE_OPENAI_REGION=koreacentral
set AZURE_OPENAI_RESOURCE_NAME=YOUR_RESOURCE_NAME
set AZURE_OPENAI_DEPLOYMENT_NAME=gpt-4.1-mini

REM Azure OpenAI API 설정 (영구적 - 사용자 환경 변수)
setx AZURE_OPENAI_API_KEY "YOUR_ACTUAL_API_KEY_HERE"
setx AZURE_OPENAI_REGION "koreacentral"
setx AZURE_OPENAI_RESOURCE_NAME "YOUR_RESOURCE_NAME"
setx AZURE_OPENAI_DEPLOYMENT_NAME "gpt-4.1-mini"
```

### 3단계: 환경 변수 설정 실행

1. **명령 프롬프트(CMD) 또는 PowerShell 실행**
2. **프로젝트 루트 디렉토리로 이동**
3. **다음 명령어 실행**:

```bash
set-env.bat
```

## 🔧 상세 설정 설명

### 환경 변수 설명

| 변수명 | 설명 | 예시 값 |
|--------|------|---------|
| `AZURE_OPENAI_API_KEY` | Azure OpenAI API 키 | `sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` |
| `AZURE_OPENAI_REGION` | Azure 리소스 지역 | `koreacentral` |
| `AZURE_OPENAI_RESOURCE_NAME` | Azure OpenAI 리소스 이름 | `oai-aiforbifproject` |
| `AZURE_OPENAI_DEPLOYMENT_NAME` | 배포된 모델 이름 | `gpt-4.1-mini` |

### set-env.bat 파일 동작 방식

1. **일시적 설정**: 현재 명령 프롬프트 세션에만 적용
2. **영구적 설정**: Windows 시스템 환경 변수로 저장 (재부팅 후에도 유지)

## 🧪 설정 확인

### 1. 환경 변수 확인

```bash
echo %AZURE_OPENAI_API_KEY%
echo %AZURE_OPENAI_REGION%
echo %AZURE_OPENAI_RESOURCE_NAME%
echo %AZURE_OPENAI_DEPLOYMENT_NAME%
```

### 2. 애플리케이션 테스트

1. **애플리케이션 실행**:
```bash
./gradlew bootRun
```

2. **API 테스트**:
   - 브라우저에서 `http://localhost:8080/api/diaries/test-connection` 접속
   - "AI 서비스 연결 성공!" 메시지 확인

## ⚠️ 주의사항

### 보안 관련

1. **API 키 보호**
   - API 키를 절대 Git에 커밋하지 마세요
   - `set-env.bat` 파일을 `.gitignore`에 추가하는 것을 권장
   - 팀원들과 API 키를 공유할 때는 안전한 방법 사용

2. **환경 변수 파일 관리**
   ```bash
   # .gitignore에 추가
   set-env.bat
   set-env-local.bat
   ```

### 문제 해결

1. **API 키 오류**
   - Azure Portal에서 API 키가 올바른지 확인
   - 리소스가 활성화되어 있는지 확인

2. **환경 변수 미적용**
   - 명령 프롬프트를 재시작
   - 시스템 재부팅 후 다시 시도

3. **권한 오류**
   - 관리자 권한으로 명령 프롬프트 실행

## 🔄 다른 운영체제 설정

### Linux/Mac 사용자

`set-env.sh` 파일 생성:

```bash
#!/bin/bash
echo "Setting up environment variables for BIF project..."

export AZURE_OPENAI_API_KEY="YOUR_API_KEY"
export AZURE_OPENAI_REGION="koreacentral"
export AZURE_OPENAI_RESOURCE_NAME="YOUR_RESOURCE_NAME"
export AZURE_OPENAI_DEPLOYMENT_NAME="gpt-4.1-mini"

echo "Environment variables set successfully!"
echo "Building the project..."
./gradlew build
```

실행:
```bash
chmod +x set-env.sh
./set-env.sh
```

## 📞 지원

문제가 발생하면 다음을 확인하세요:

1. **Azure OpenAI 서비스 상태**: https://status.azure.com
2. **프로젝트 이슈**: GitHub Issues
3. **팀 내부 지원**: 팀 채널 또는 이메일

## 📝 체크리스트

- [ ] Azure OpenAI 서비스 생성 완료
- [ ] API 키 및 엔드포인트 확인
- [ ] set-env.bat 파일 수정
- [ ] 환경 변수 설정 실행
- [ ] 설정 확인 (echo 명령어)
- [ ] 애플리케이션 실행 테스트
- [ ] API 연결 테스트 성공

---

**마지막 업데이트**: 2024년 12월
**작성자**: BIF 개발팀 