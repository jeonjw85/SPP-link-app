
# SPP Link App (Android)

HC-06과 같은 Serial Port Profile 블루투스 모듈을 사용하여 안드로이드 기기와 데이터를 송수신하는 애플리케이션

## 주요 기능

*   **블루투스 연결 관리**: SPP 기기와 안정적인 블루투스 연결 시도 및 유지
*   **데이터 송수신**: 모듈과 실시간으로 데이터를 주고받으며, 수신된 데이터는 UI에 즉시 표시

## 프로젝트 구조 개요

- **`app/src/main/java/kr.jjw.spp_link_app`**: 핵심 로직이 담긴 Kotlin 코드
- **`AndroidManifest.xml`**: 앱의 권한 및 컴포넌트 정의 파일
- **`build.gradle.kts`**: 종속성 및 빌드 타입 관리
- **`local.properties`**: 프로젝트별 환경 변수 저장 파일

## Getting Started

### 1. 필수 준비 사항

*   Android Studio 설치 완료
*   Java Development Kit (JDK) 21 설치 확인

### 2. 설정 단계

1.  **클론**: Git을 사용하여 프로젝트를 로컬 환경으로 복제합니다.
    ```bash
    git clone https://github.com/jeonjw85/SPP-link-app
    cd SPP-link-app
    ```
2.  **환경 변수 설정**: `local.properties.example` 파일을 복사 후 **`local.properties`** 파일생성
3.  **값 채우기**: `local.properties`

    *   `sdk.dir`: 사용 중인 Android SDK의 설치 경로를 지정
    *   `TARGET_MAC_ADDRESS`: 연결하고자 하는 SPP 모듈의 정확한 MAC 주소를 입력

4.  **빌드**: Android Studio에서 프로젝트를 열고 **Sync Project with Gradle Files** 후 실행하여 빌드 진행

## 기술 스택 및 환경

*   **언어**: Kotlin
*   **Java SDK 버전**: 21
*   **Kotlin API 레벨**: 2.0
*   **Android Target SDK**: 34
*   **빌드 시스템**: Gradle (Kotlin DSL)