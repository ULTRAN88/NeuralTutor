# NeuralTutor Pro

NeuralTutor Pro is a Java 21 desktop intelligent tutoring system built around a research-oriented adaptive loop:

**Observe learner → update learner model → estimate cognitive state → choose intervention → teach/practice → observe again**

## What is actually different from a normal quiz?

A conventional quiz stores correctness. NeuralTutor also observes within-session behavioral signals:

- response latency
- option changes (corrections/hesitation proxy)
- correctness
- item difficulty
- current topic mastery

The system keeps **two separate states**:

1. **Knowledge state** — estimated mastery for each Java skill.
2. **Cognitive state** — estimated current task load (`Flow`, `Stretch`, `High Load`, `Overload`).

The adaptive policy uses both states. Example:

- high mastery + low load → increase difficulty
- high load → scaffold with a hint
- overload + weak prerequisite → route backward to the prerequisite
- low mastery but manageable load → give a micro-learning recommendation
- otherwise → targeted practice

## Product areas

- Dashboard / learning cockpit
- Learn Java micro-lessons
- Adaptive Practice
- Progress Intelligence
- Mistake Notebook
- Research Lab
- CSV export of interaction telemetry
- Persistent local learner profile using SQLite

## Java concepts demonstrated

- OOP / inheritance / abstract classes
- interface-driven policy
- Strategy-like adaptive policy
- observer-style UI refresh
- `ScheduledExecutorService` for live session monitoring
- `ForkJoinPool` for parallel question ranking
- thread-safe learner state
- JDBC + SQLite + repository/DAO separation
- generics, collections, records and exceptions
- MVC-like separation between model, engine, persistence and UI

## Build on Windows

Requirements: Java 21+.

```powershell
.\build.ps1
java -jar .\dist\NeuralTutor-Pro.jar
```

## Build on Linux/macOS

```bash
bash build.sh
java -jar dist/NeuralTutor-Pro.jar
```

## Run the engine self-test

```bash
java -cp build/classes:lib/sqlite-jdbc.jar com.neuraltutor.TestEngine
```

On PowerShell:

```powershell
java -cp "build\classes;lib\sqlite-jdbc.jar" com.neuraltutor.TestEngine
```

## Data

The application creates `neuraltutor_pro.db` beside the application at runtime.

The 100-question Java bank from the original project is preserved and reused as the initial assessment/practice corpus.

## Research framing

Do not claim that NeuralTutor is the first adaptive learning platform to use behavioral signals. The defensible contribution is the architecture being investigated:

> a Java-based real-time tutoring loop that combines response latency, correction behavior, correctness and concept difficulty with a skill graph and dual-state learner model, then changes the next instructional action at question-level granularity.

This is a prototype/research system, not a validated cognitive measurement instrument. The cognitive state is a behavioral proxy and should be evaluated empirically against learning outcomes and/or validated cognitive-load measures in a research study.