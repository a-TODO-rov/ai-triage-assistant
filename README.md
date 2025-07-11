# 🤖 AI-Powered GitHub Issue Triage Assistant

An intelligent assistant that automates GitHub issue triage using AI, Redis Stack, and Spring Boot.

This project was built for **AI Week @ Redis** to streamline the process of managing and labeling GitHub issues across client libraries like Jedis and Lettuce.

---

## 📌 What It Does

- 🏷️ **Auto-labels new GitHub issues** using LLMs (GPT-4, Claude, Gemini via [LiteLLM](https://github.com/BerriAI/litellm))
- 🔍 **Finds similar past issues** using Redis vector search (RediSearch + HNSW)
- 🔔 **Sends Slack notifications** to relevant maintainers
- 💬 *(Optional)* Posts enriched triage results as GitHub comments
- 🧠 *(Planned)* Stack trace root cause analysis
- 🧪 *(Planned)* Test coverage gap detection

---

## 🧱 Tech Stack

| Component        | Tool/Service                        |
|------------------|-------------------------------------|
| Backend          | Java 24, Spring Boot 3.5            |
| LLM Integration  | [LiteLLM](https://github.com/BerriAI/litellm) proxy to GPT-4 / Claude / Gemini |
| Redis Vector DB  | Redis Stack (`redis/redis-stack-server`) |
| Redis Client     | [JRedisStack](https://github.com/redis/jedis/tree/master/jedis-redis-stack) |
| GitHub           | Webhook API + GitHub Java API       |
| Slack            | Incoming Webhooks                   |
| Testing          | JUnit 5, Testcontainers, Mockito    |

---

## 🏗️ Architecture Overview

```text
[ GitHub Webhook ]
      |
      v
[ Spring Boot API ]
   ├── LabelingService          → calls LiteLLM
   ├── RedisVectorStoreService → stores issue vectors in Redis
   ├── SemanticSearchService   → finds similar issues
   └── SlackNotifier           → sends Slack alerts
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21 or 24
- Maven 3.9+
- Docker (for Redis Stack via Testcontainers)
- Slack Webhook URL
- LiteLLM endpoint + API key (for OpenAI, Claude, or Gemini)

### Clone & Build

```bash
git clone https://github.com/your-org/ai-triage-assistant.git
cd ai-triage-assistant
./mvnw clean install
```

---

## 🧪 Running Tests

Integration tests spin up a full Redis Stack container:

```bash
./mvnw test
```

---

## ✅ Features Roadmap

- [ ] Auto-labeling via LLM
- [ ] Redis vector similarity search
- [ ] Slack integration
- [ ] Testcontainers-based integration tests
- [ ] Root cause analysis from stack traces
- [ ] Test coverage detection
- [ ] GitHub comment bot

---