# DGKT: Dependency Graph-enhanced Knowledge Tracing

A small quiz web app that finds the root cause of a weak topic. It tracks a student's knowledge of each topic with Bayesian Knowledge Tracing (BKT) and walks back through a topic dependency graph to find which earlier topic is the real problem.

Built with Java 17 and Spring Boot. The quiz pages are plain HTML and JavaScript.

## Features
- **Assessment:** short test on chosen topics, then follow-up questions on prerequisites, then a root-cause diagnosis.
- **Practice:** one topic at a time, the right answer is shown after each question.
- **Dashboard:** topic mastery, weak topics, past sessions, and a button to clear all data.
- No login. The browser keeps a random student code.

## Run locally
```
mvn spring-boot:run
```
Open http://localhost:8080/ . Data is saved in `data/` (H2 file database), which is created on the first start.

Run the tests with `mvn test`.

## Project layout
- `core/` BKT, dependency graph, student model, root-cause logic
- `quiz/` assessment engine and practice service
- `store/` database entities and repositories
- `web/` REST controllers
- `sim/` simulated-student experiments
- `src/main/resources/static/` web pages
- `src/main/resources/data/` graph and question bank

## Deploy on Render
1. Push the repo to GitHub.
2. On Render, create a Web Service from the repo. It uses the `Dockerfile`.
3. Optional: set an environment variable `RESET_PIN`. The dashboard's "Clear all data" button then asks for this PIN.

The free plan does not keep the disk, so data is lost on restart. To start fresh for a new tester, open `/dashboard.html` and press **Clear all data**.
