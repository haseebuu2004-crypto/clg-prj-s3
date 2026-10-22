# Skill Swap

Skill Swap is a Java Swing desktop application for students who want to teach one another. It supports student profiles, skill preferences, direct and chain matching, session scheduling, completion, ratings, and local persistence.

## Run the application

Requirements: Java 17 or newer (a JDK, so `java`, `javac`, and `jar` are available). No server, database installation, build tool, or network connection is required.

On Windows, double-click `run.bat` or run this command from the project folder:

```bat
run.bat
```

The launcher compiles the sources and starts the desktop application. To only compile it:

```bat
build.bat
```

The first launch creates demo profiles and session history. Data is saved atomically to `%USERPROFILE%\.skillswap\skillswap-data.bin`. The Profile & join page can add any missing demo profiles without replacing saved profiles or sessions.

## Compact screen layout

The Swing interface adapts to narrow, portrait-sized windows: the sidebar becomes a page selector, card grids stack into fewer columns, session/profile details switch to vertical layouts, and the registration form reflows while keeping controls readable and comfortably sized. This remains a desktop Java application; it is not packaged as an Android or iOS app.

## Demo profiles

| Student | Can teach | Wants to learn | Use in the demo |
|---|---|---|---|
| Asha Rao | Java, Advanced | Python, Intermediate | Direct match with Dev |
| Dev Nair | Python, Advanced | Java, Advanced | Reciprocal Java/Python exchange |
| Meera Nair | Java, Intermediate | Python, Intermediate | Start of the three-person chain |
| Arjun Menon | Python, Advanced | SQL, Intermediate | Middle of the chain |
| Kabir Shah | SQL, Advanced | Java, Intermediate | Closes the chain |
| Tara Joseph | French, Advanced | Graphic Design, Beginner | No-match example |

Two completed, rated sample sessions give the dashboard real activity on first launch. Demo profiles are clearly labelled by their `@skillswap.demo` addresses.

## Matching rules

`DirectSwapStrategy` requires a reciprocal exchange: each student must teach a skill the other wants, and each teacher's proficiency must meet the learner's requested level. Asha and Dev demonstrate this with Java and Python.

When no direct result is available, `MatchEngine` runs `ChainSwapStrategy`. A directed edge from learner A to teacher B means B offers a skill A wants at a sufficient level. A valid chain is a closed cycle of 3 or 4 distinct students. The results show each teacher, learner, and skill; scheduling a chain creates one linked session per cycle edge.

Compatibility is a hard requirement. Direct results rank by the number of reciprocal skill exchanges, then partner teaching rating. Chain results rank shorter cycles first, then the average teaching rating of the other participants. A student with no ratings receives a neutral 3.0 ranking baseline; no match score or percentage is displayed.

## OOP and architecture

- `Person` is abstract; `Student` inherits its identity and contact fields.
- `MatchStrategy` is implemented by `DirectSwapStrategy` and `ChainSwapStrategy`. `MatchEngine` calls both through the interface.
- Model data is private and accessed through validated methods. `Session` enforces its lifecycle transitions.
- `SkillSwapService` owns validation and workflows. `LocalStore` persists students, skills, sessions, and ratings independently of Swing.
- `MainFrame` builds the screens and calls the service; it does not contain matching rules.

## Run the acceptance checks

```bat
test.bat
```

The dependency-free checks cover direct matching, no-match handling, chain validity, proficiency, ranking, duplicate records, session transitions, ratings, and persistence after reload.

## Known limitations

- This is a local college demonstration, not a multi-device service. Profiles are selected locally and are not authenticated accounts.
- A rating is collected from the learner for each completed teaching session. There is no messaging system or live calendar integration.
- Local records are stored in a Java binary file under the current Windows user's profile.
