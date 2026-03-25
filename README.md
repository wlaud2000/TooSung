# 투성투성 (TooSungTooSung) 📰

> AI 기반 주식 뉴스 · 공시 분석 비서 — 내 관심 종목 뉴스, AI가 알아서 읽고 나한테 맞게 정리해드려요

<br>

## 📌 프로젝트 소개

개인 투자자는 매일 쏟아지는 뉴스와 공시를 직접 읽고 해석하기가 어렵습니다.

**투성투성**은 이 문제를 해결하기 위해 만든 AI 기반 금융 뉴스 분석 서비스입니다.

- 관심 종목의 뉴스를 자동 수집하고 **호재/악재를 근거와 함께 판단**합니다
- 어려운 공시를 **쉬운 말로 번역**하고 투자 포인트를 짚어줍니다
- **RAG** 기반으로 사용자의 과거 관심사를 학습해 **개인화된 아침 브리핑**을 제공합니다
- "삼성전자 이번 주 악재 뭐 있어?"처럼 **자연어로 질문**할 수 있습니다

<br>

## 🛠 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java_21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4169E1?style=flat-square&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)

### Frontend
![React](https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)

### AI / RAG
![OpenAI](https://img.shields.io/badge/OpenAI_GPT--4-412991?style=flat-square&logo=openai&logoColor=white)
![LangChain](https://img.shields.io/badge/LangChain-1C3C3C?style=flat-square&logo=langchain&logoColor=white)
![Qdrant](https://img.shields.io/badge/Qdrant-Vector_DB-DC244C?style=flat-square)

### External APIs
- **DART OpenAPI** — 전자공시 수집
- **네이버 뉴스 API / Google News RSS** — 뉴스 수집
- **Yahoo Finance API** — 주가 및 종목 정보

<br>

## 🏗 시스템 아키텍처

<img width="1651" height="1161" alt="투성투성전체시스템아키텍처 drawio " src="https://github.com/user-attachments/assets/439748a5-445a-4351-871f-4efb88a1a3aa" />

<br>

## 📊 ERD

<img width="1406" height="757" alt="투성투성 ERD (2)" src="https://github.com/user-attachments/assets/5402807a-5177-4d60-a4c4-2a4ca36f1d19" />


<br>

## 🚀 로컬 실행 방법

### 사전 요구사항

- Java 21
- Docker & Docker Compose (MySQL, Redis, Qdrant)

### 1. 저장소 클론

```bash
git clone https://github.com/wlaud2000/TooSung.git
cd TooSung
```

### 2. 환경 변수 설정

```bash
cp .env.example .env
# .env 파일에 아래 값 입력
# OPENAI_API_KEY=
# DART_API_KEY=
# NAVER_CLIENT_ID=
# NAVER_CLIENT_SECRET=
```

### 3. 인프라 실행

```bash
docker-compose up -d
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

<br>

## 📁 프로젝트 구조

```
tuseong-tuseong/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/tuseong/
│   │   │       ├── domain/          # 도메인별 패키지 (news, disclosure, user, rag...)
│   │   │       └── global/          # 공통 설정, 예외 처리, 보안
│   │   │      
│   │   └── resources/
│   │       └── application.yml
└── docker-compose.yml
```

<br>

## 🔑 주요 기술적 도전과 해결

### 1. RAG 기반 개인화 브리핑

단순 키워드 매칭이 아닌 사용자의 과거 클릭 패턴, 질문 이력을 임베딩해 Vector DB에 저장합니다. 브리핑 생성 시 오늘의 뉴스와 유사도 검색을 수행해 개인에게 가장 관련 있는 콘텐츠를 우선 제공합니다.

### 2. 공시 자동 해석 파이프라인

DART API로 수집한 공시 원문을 LLM에 전달해 "쉬운 말 번역 + 투자 포인트" 구조로 재가공합니다. 프롬프트 설계 시 전문용어 해석 일관성을 유지하기 위해 Few-shot 예시를 활용했습니다.

### 3. 호재/악재 판단 + 근거 제시

감성 점수만 반환하는 기존 방식 대신, LLM이 판단 근거를 함께 생성하도록 Chain-of-Thought 프롬프팅을 적용했습니다.

<br>

## 🙋 개발자

| 이름 | 역할 | GitHub |
|------|------|--------|
| 김지명 | Backend / AI | [@wlaud2000](https://github.com/wlaud2000) |

<br>
