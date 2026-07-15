# 🏋️ GymTracker Backend

A secure and scalable backend application built with **Java** and **Spring Boot** that helps users track workouts, exercises, and fitness progress. The project follows a clean layered architecture and implements JWT-based authentication for secure access to REST APIs.

---

## 🚀 Features

- 🔐 JWT Authentication & Authorization
- 👤 User Registration & Login
- 🏋️ Workout Management
- 💪 Exercise Tracking
- 📊 Progress Tracking (In Progress)
- ✅ CRUD Operations
- 📦 Layered MVC Architecture
- ⚠️ Global Exception Handling
- 📄 DTO-based Request & Response Handling
- ✔️ Input Validation
- 📖 Swagger API Documentation

---

## 🛠 Tech Stack

### Backend

- Java 21
- Spring Boot
- Spring Security 6
- Spring Data JPA
- Hibernate ORM

### Database

- MySQL

### Tools

- Maven
- Git
- GitHub
- Postman
- Swagger/OpenAPI
- IntelliJ IDEA

---

## 📂 Project Structure

```
src
 ├── controller
 ├── service
 ├── repository
 ├── entity
 ├── dto
 ├── security
 ├── config
 ├── exception
 └── util
```

The project follows a layered architecture:

```
Client
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
Database
```

---

## 🔑 Authentication

The application uses **JWT (JSON Web Tokens)** for authentication.

Flow:

```
User Register/Login
        ↓
Spring Security
        ↓
Generate JWT Token
        ↓
Client Stores Token
        ↓
Token Sent in Authorization Header
        ↓
Protected APIs Accessed
```

Authorization Header:

```
Authorization: Bearer <JWT_TOKEN>
```

---

## 📌 Current Modules

### User Module

- Register User
- Login User
- Password Encryption (BCrypt)

### Workout Module

- Create Workout
- Update Workout
- Delete Workout
- View User Workouts

### Exercise Module

- Add Exercises
- Update Exercises
- Delete Exercises

### Progress Module

- Track Weight
- Workout Progress
- Coming Soon...

---

## 📡 REST APIs

### Authentication

| Method | Endpoint |
|---------|----------|
| POST | `/auth/register` |
| POST | `/auth/login` |

---

### User

| Method | Endpoint |
|---------|----------|
| GET | `/users/{id}` |
| PUT | `/users/{id}` |
| DELETE | `/users/{id}` |

---

### Workout

| Method | Endpoint |
|---------|----------|
| POST | `/users/{id}/workouts` |
| GET | `/users/{id}/workouts` |
| PUT | `/workouts/{id}` |
| DELETE | `/workouts/{id}` |

---

## ⚙️ Installation

Clone the repository

```bash
git clone https://github.com/yuvnotcode/gymtracker.git
```

Move into the project

```bash
cd gymtracker
```

Configure your database in

```
application.yml
```

Run the application

```bash
mvn spring-boot:run
```

---

## 🧪 Testing

API endpoints are tested using **Postman**.

Swagger UI:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 📈 Upcoming Features

- 📊 Analytics Dashboard
- 🔥 Calories Tracking
- 📅 Workout Calendar
- 📈 Progress Charts
- 📷 Profile Images
- 📱 Email Verification
- 🔄 Password Reset
- 🏆 Fitness Goals
- 👥 Role-Based Authorization
- 🐳 Docker Support
- ☁️ Deployment
- ✅ Unit & Integration Testing

---

## 💡 What I Learned

Through this project I gained hands-on experience with:

- Spring Boot
- Spring Security
- JWT Authentication
- REST API Development
- Hibernate & JPA
- MySQL
- Layered Architecture
- Dependency Injection
- DTO Pattern
- Exception Handling
- API Testing with Postman
- Git & GitHub

---

## 👨‍💻 Author

**Yuvaditya Verma**

LinkedIn:
https://linkedin.com/in/yuvaditya-verma

GitHub:
https://github.com/yuvnotcode

Email:
yuva8954@gmail.com

---

## ⭐ If you found this project interesting, consider giving it a star!
