# Progressify – Development Roadmap

Progressify is a mobile **LifeRPG productivity application** built in **Android Studio using Kotlin**.  
The goal of the project is to gamify everyday tasks by rewarding users with **experience points, levels, streaks and skill progression**.

This document describes the planned development roadmap and work distribution.

---

# Project Roles

### Frontend
Responsible for:
- UI / UX
- Navigation
- Screens and layouts
- User interaction
- Connecting UI with ViewModels

Technologies:
- Kotlin
- Jetpack Compose
- Android Navigation
- MVVM architecture

---

### Backend
Responsible for:
- Application logic
- Data models
- XP and leveling system
- Firebase integration
- Data synchronization

Technologies:
- Kotlin
- Firebase Firestore
- Firebase Authentication
- Repository pattern

---

# Authors

**Progressify** was created by third-year students of  
**Applied Computer Science and Measurement Systems**  
at the **Faculty of Physics and Astronomy, University of Wrocław**.

The application was developed as part of the course **Mobile Application Project**.

## Development Team

| Role | Avatar | Name | GitHub |
|------|-------|------|--------|
| Frontend Developer | <img src="https://github.com/Manorekk.png" width="40"/> | **Bartosz Lubański** | [GitHub](https://github.com/Manorekk) |
| Backend Developer | <img src="https://github.com/AdrianOrynski.png" width="40"/> | **Adrian Oryński** | [GitHub](https://github.com/AdrianOrynski) |

---

# Technology Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| IDE | Android Studio |
| Architecture | MVVM |
| Database | Firebase Firestore |
| Authentication | Firebase Auth |
| UI | Jetpack Compose |

---

# Development Roadmap

## Week 1 — Project Setup

### Backend
- Create Firebase project
- Configure Firestore
- Configure Firebase Authentication
- Design database structure
- Create basic data models

### Frontend
- Create Android project
- Configure Jetpack Compose
- Setup navigation
- Create basic screen layouts

Screens:
- Login
- Dashboard
- Task List
- Profile

---

## Week 2 — User Authentication

### Backend
- Implement authentication logic
- Support:
  - Email login
- Store user profiles in Firestore

### Frontend
- Login screen
- Registration screen
- Navigation after login

---

## Week 3 — Task System

### Backend
- Implement task model
- CRUD operations for tasks
  - Create
  - Read
  - Update
  - Delete
- Store tasks in Firestore

### Frontend
- Task list screen
- Add task screen
- Task editing
- Task completion toggle

---

## Week 4 — XP System

### Backend
- Implement XP calculation
- Implement level progression
- Update XP when tasks are completed


### Frontend
- XP progress bar
- User level display
- XP gain animation

---

## Week 5 — Skills / Categories

### Backend
- Implement skill categories
- Assign tasks to skills
- Skill level system

Example skills:

- Fitness
- Knowledge
- Productivity
- Creativity

### Frontend
- Skills screen
- Skill level display
- Skill progress bars

---

## Week 6 — Streak System

### Backend
- Implement daily streak tracking
- Update streak after task completion
- Streak bonus system

### Frontend
- Streak counter
- Dashboard streak display
- Visual streak indicators

---

## Week 7 — Character Classes

### Backend
- Implement class system
- XP bonuses based on category

Example classes:

- Warrior (Fitness bonus)
- Scholar (Knowledge bonus)
- Artisan (Creativity bonus)
- Balanced (small global bonus)

### Frontend
- Class selection screen
- Class description UI

---

## Week 8 — Statistics

### Backend
- Aggregate user data
- Calculate statistics

Examples:
- XP per week
- Tasks completed
- Skill growth

### Frontend
- Statistics screen
- Graphs and charts

---

## Week 9 — UI Polish & Improvements

### Backend
- Error handling
- Data validation
- Performance improvements

### Frontend
- UI improvements
- Animations
- Loading states
- UX improvements

---

## Week 10 — Testing & Finalization

### Tasks
- End-to-end testing
- Fix bugs
- Improve UI consistency
- Prepare project presentation

Deliverables:
- Working mobile application
- GitHub repository
- Documentation
- Demo scenario

---

# Minimum Viable Product (MVP)

If development time becomes limited, the minimum version should include:

- User authentication
- Task management
- XP system
- Level progression
- Streak tracking

Optional features:
- Classes
- Advanced statistics
- Achievements
