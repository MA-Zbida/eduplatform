# 🎓 Intelligent Educational Platform

An intelligent, secure educational platform built with Spring Boot, combining classical web application concerns with advanced AI concepts including Large Language Models (LLM), Retrieval-Augmented Generation (RAG), and Agentic AI for supervision and decision-making.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Getting Started](#getting-started)
- [Demo Credentials](#demo-credentials)
- [Usage Guide](#usage-guide)
- [AI Components](#ai-components)
- [Configuration](#configuration)
- [API Reference](#api-reference)

## ✨ Features

### Administrator Features
- **Student Management**: Create, update, and manage student accounts
- **Course Management**: Create, edit, publish, and archive courses
- **RAG Indexing**: Trigger content indexing for AI-powered quiz generation
- **Enrollment Management**: Assign students to courses
- **Dashboard**: View platform statistics and recent activity

### Student Features
- **Course Access**: View and study enrolled courses
- **AI Quiz Generation**: Generate personalized quizzes based on course content
- **Adaptive Difficulty**: Quiz difficulty adjusts based on performance
- **Progress Tracking**: Monitor learning progress and quiz history
- **Detailed Feedback**: Receive AI-generated feedback on quiz results

### AI-Powered Features
- **Intelligent Quiz Generation**: LLM creates contextually relevant questions
- **RAG Integration**: Questions are based on actual course content
- **Agentic AI Supervision**: AI agent orchestrates the entire learning flow
- **Adaptive Learning**: Difficulty automatically adjusts to student performance

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                           │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐   │
│  │   Thymeleaf │  │  Controllers │  │   Spring Security     │   │
│  │   Templates │  │  (Auth/Admin │  │   (Authentication/    │   │
│  │   (HTML5)   │  │   /Student)  │  │    Authorization)     │   │
│  └─────────────┘  └──────────────┘  └───────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                               │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐   │
│  │   User      │  │   Course     │  │   Quiz Service        │   │
│  │   Service   │  │   Service    │  │   + Agent Delegation  │   │
│  └─────────────┘  └──────────────┘  └───────────────────────┘   │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐   │
│  │ Enrollment  │  │  Dashboard   │  │   RAG Service         │   │
│  │   Service   │  │  Service     │  │   (Content Indexing)  │   │
│  └─────────────┘  └──────────────┘  └───────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      AI/AGENT LAYER                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    AGENTIC AI                             │   │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────────┐  │   │
│  │  │   Agent    │  │    LLM     │  │    RAG Context     │  │   │
│  │  │  Service   │◄─│  Service   │◄─│    Retrieval       │  │   │
│  │  │ (Orchestr.)│  │ (GPT/Mock) │  │    Service         │  │   │
│  │  └────────────┘  └────────────┘  └────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      DATA LAYER                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐   │
│  │ Repositories│  │   Entities   │  │   H2 Database         │   │
│  │ (JPA/Spring │  │   (User,     │  │   (In-Memory /        │   │
│  │    Data)    │  │  Course,etc) │  │    PostgreSQL)        │   │
│  └─────────────┘  └──────────────┘  └───────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## 🛠️ Technologies

| Category | Technology |
|----------|------------|
| Framework | Spring Boot 3.2.1 |
| Security | Spring Security 6 |
| Database | Spring Data JPA, H2 (dev), PostgreSQL (prod) |
| View Layer | Thymeleaf, Bootstrap 5, Bootstrap Icons |
| AI Integration | Spring AI 0.8.1, OpenAI GPT-4 (mock mode available) |
| Build Tool | Maven |
| Java Version | Java 21 |

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- (Optional) OpenAI API key for real LLM integration

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd demo
   ```

2. **Build the project**
   ```bash
   ./mvnw clean install
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Access the application**
   - Open: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console

## 🔐 Demo Credentials

| Role | Username | Password |
|------|----------|----------|
| Administrator | `admin` | `admin123` |
| Student | `student1` | `student123` |
| Student | `student2` | `student123` |
| Student | `student3` | `student123` |

## 📖 Usage Guide

### Administrator Workflow

1. **Login** as administrator
2. **Create Courses**: Navigate to Courses → Add New Course
3. **Add Content**: Write or paste educational content
4. **Publish Course**: Change status to Published
5. **Index for RAG**: Click "Index for AI" to enable quiz generation
6. **Manage Students**: Create student accounts
7. **Enroll Students**: Assign students to courses

### Student Workflow

1. **Login** as a student
2. **View Dashboard**: See enrolled courses and progress
3. **Access Course**: Click on a course to view content
4. **Generate Quiz**: Select difficulty and number of questions
5. **Take Quiz**: Answer all questions
6. **View Results**: See score, feedback, and explanations
7. **Track Progress**: Monitor improvement over time

## 🤖 AI Components

### 1. Large Language Model (LLM) Service

The LLM Service handles communication with language models for quiz generation and evaluation.

**Features:**
- Mock mode for development (no API costs)
- Real OpenAI integration when configured
- Structured prompts for consistent output
- Quiz generation with explanations

**Configuration:**
```properties
# Enable mock mode (default)
app.llm.mock-mode=true

# For real OpenAI integration
app.llm.mock-mode=false
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

### 2. Retrieval-Augmented Generation (RAG)

RAG enhances quiz generation by retrieving relevant course content.

**How it works:**
1. Course content is chunked into segments (500 chars, 50 overlap)
2. Chunks are stored with course reference
3. When generating quiz, relevant chunks are retrieved
4. LLM uses chunks as context for question generation

**Indexing Process:**
```
Course Content → Chunking → Storage → Ready for Retrieval
```

### 3. Agentic AI (Agent Service)

The Agent Service orchestrates the entire learning process with intelligent decision-making.

**Responsibilities:**
- Determine appropriate quiz difficulty based on student history
- Coordinate between RAG and LLM services
- Evaluate quiz results and provide personalized feedback
- Update enrollment progress based on performance
- Recommend next steps for learning

**Decision Flow:**
```
Student Request
      │
      ▼
┌─────────────────┐
│ Analyze History │─────────────────┐
└────────┬────────┘                 │
         │                          │
         ▼                          ▼
┌─────────────────┐      ┌─────────────────┐
│ Determine       │      │ Retrieve RAG    │
│ Difficulty      │      │ Context         │
└────────┬────────┘      └────────┬────────┘
         │                        │
         └────────┬───────────────┘
                  │
                  ▼
         ┌─────────────────┐
         │ Generate Quiz   │
         │ via LLM         │
         └────────┬────────┘
                  │
                  ▼
         ┌─────────────────┐
         │ Return Quiz to  │
         │ Student         │
         └─────────────────┘
```

**Adaptive Difficulty Algorithm:**
```
Average of last 5 quiz scores:
  ≥ 90%  → EXPERT
  ≥ 75%  → HARD
  ≥ 50%  → MEDIUM
  < 50%  → EASY
```

## ⚙️ Configuration

### Application Properties

```properties
# Server
server.port=8080

# Database (Development)
spring.datasource.url=jdbc:h2:mem:eduplatform
spring.h2.console.enabled=true

# AI Configuration
app.llm.mock-mode=true

# RAG Settings
app.rag.chunk-size=500
app.rag.chunk-overlap=50
app.rag.max-chunks-per-query=5

# Quiz Settings
app.quiz.pass-threshold=70
app.quiz.min-quizzes-to-complete=2
```

### Production Configuration

For production deployment, update `application.properties`:

```properties
# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/eduplatform
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate

# Disable H2 Console
spring.h2.console.enabled=false

# Thymeleaf caching
spring.thymeleaf.cache=true

# Real OpenAI
app.llm.mock-mode=false
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

## 📊 API Reference

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/login` | Login page |
| POST | `/login` | Authenticate user |
| POST | `/logout` | Logout user |

### Admin Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/dashboard` | Admin dashboard |
| GET | `/admin/students` | List students |
| POST | `/admin/students` | Create student |
| GET | `/admin/courses` | List courses |
| POST | `/admin/courses` | Create course |
| POST | `/admin/courses/{id}/publish` | Publish course |
| POST | `/admin/courses/{id}/index` | Index for RAG |

### Student Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/student/dashboard` | Student dashboard |
| GET | `/student/courses` | List enrolled courses |
| GET | `/student/courses/{id}` | View course |
| POST | `/student/courses/{id}/quiz/generate` | Generate quiz |
| GET | `/student/quizzes/{id}` | Take quiz |
| POST | `/student/quizzes/{id}/submit` | Submit quiz |
| GET | `/student/quizzes/{id}/result` | View result |

## 📁 Project Structure

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── config/
│   └── DataLoader.java
├── controller/
│   ├── AdminController.java
│   ├── AuthController.java
│   └── StudentController.java
├── dto/
│   ├── CourseDTO.java
│   ├── DashboardStatsDTO.java
│   ├── QuizRequestDTO.java
│   ├── QuizSubmissionDTO.java
│   └── UserDTO.java
├── entity/
│   ├── AnswerOption.java
│   ├── Course.java
│   ├── CourseChunk.java
│   ├── CourseStatus.java
│   ├── DifficultyLevel.java
│   ├── Enrollment.java
│   ├── EnrollmentStatus.java
│   ├── Question.java
│   ├── Quiz.java
│   ├── QuizResult.java
│   ├── Role.java
│   ├── StudentAnswer.java
│   └── User.java
├── repository/
│   ├── AnswerOptionRepository.java
│   ├── CourseChunkRepository.java
│   ├── CourseRepository.java
│   ├── EnrollmentRepository.java
│   ├── QuestionRepository.java
│   ├── QuizRepository.java
│   ├── QuizResultRepository.java
│   └── UserRepository.java
├── security/
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── SecurityConfig.java
│   └── SecurityUtils.java
└── service/
    ├── AgentService.java
    ├── CourseService.java
    ├── DashboardService.java
    ├── EnrollmentService.java
    ├── LLMModels.java
    ├── LLMService.java
    ├── QuizService.java
    ├── RAGService.java
    └── UserService.java

src/main/resources/
├── application.properties
├── static/
├── templates/
│   ├── layout.html
│   ├── login.html
│   ├── error/
│   │   └── access-denied.html
│   ├── admin/
│   │   ├── dashboard.html
│   │   ├── students/
│   │   │   ├── list.html
│   │   │   └── form.html
│   │   └── courses/
│   │       ├── list.html
│   │       ├── form.html
│   │       └── view.html
│   └── student/
│       ├── dashboard.html
│       ├── history.html
│       ├── courses/
│       │   ├── list.html
│       │   └── view.html
│       └── quizzes/
│           ├── list.html
│           ├── take.html
│           └── result.html
```

## 🔒 Security Design

### Authentication
- Form-based login with CSRF protection
- Password encoding with BCrypt
- Session management with timeout

### Authorization
- Role-based access control (ADMINISTRATOR, STUDENT)
- URL-based security rules
- Custom success handler for role-based redirects

### Security Rules
```
/admin/**  → ADMINISTRATOR only
/student/** → STUDENT only
/login, /css/**, /js/** → Public
```

## 📝 License

This project is for educational purposes.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

---

**Built with ❤️ using Spring Boot, Spring Security, and AI**
