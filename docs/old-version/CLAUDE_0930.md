# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.6 community application using:
- Java 24
- Gradle build system
- Spring Web and Web Services
- MySQL database with JPA/Hibernate
- Lombok for reducing boilerplate

Package structure: `com.ktb.community`

## 프로젝트 문서

이 프로젝트는 체계적인 문서화를 통해 관리됩니다:

- **@docs/PLAN.md**: 전체 프로젝트 구현 계획 및 단계별 로드맵
- **@docs/PRD.md**: 제품 요구사항 명세서 (Product Requirements Document)
- **@docs/LLD.md**: 저수준 설계 문서 (Low Level Design) - 기술적 구현 상세
- **@docs/DDL.md**: 데이터베이스 스키마 정의 (MySQL DDL 스크립트)
- **@docs/API.md**: REST API 엔드포인트 명세
- **@Users/jsh/.claude/MCP_Context7.md** : mcp 관련 문서

모든 개발은 이 문서들을 기반으로 진행되며, 변경사항 시 문서도 함께 업데이트합니다.

## Essential Commands

### Build and Run
```bash
./gradlew build              # Build the project
./gradlew bootRun            # Run the application
./gradlew clean              # Clean build artifacts
```

### Testing
```bash
./gradlew test               # Run all tests
./gradlew test --tests ClassName.methodName  # Run specific test
./gradlew test --rerun-tasks # Force re-run tests
```

### Development
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'  # Run with dev profile
./gradlew build -x test      # Build without running tests
./gradlew tasks              # List all available tasks
```

## Database Configuration

The application connects to MySQL:
- Database: `community`
- URL: `jdbc:mysql://localhost:3306/community`
- Hibernate DDL mode: `update` (modify schema automatically)
- SQL logging: enabled in debug mode
- Tables: docs/DDL.md 파일 참조하여 스키마 확인

**Important**:
- The application.yaml contains database credentials. Never commit sensitive credentials to version control.
- 프로덕션 환경에서는 환경 변수 사용 권장 (예: `${DB_PASSWORD}`)

# MCP Documentation
@Users/jsh/.claude/MCP_Context7.md