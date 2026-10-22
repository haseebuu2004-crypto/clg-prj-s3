# Skill Swap Class Demonstration

## Before class

1. Open the project folder and double-click `run.bat`.
2. Wait for `Build successful` and the Skill Swap window.
3. Start on the Dashboard. The first launch already contains demo profiles and two completed sample sessions.
4. If profiles are missing, open **Profile & join** and choose **Add missing demo data**. This adds missing demo rows without replacing existing records.

## Suggested 5–7 minute walkthrough

### 1. Dashboard and saved profiles

Show the actual skills, open sessions, rating, recommendations, and recent activity. Use the profile selector in the upper-right corner to switch students for the local demonstration. It is a profile selector, not secure sign-in.

### 2. Direct match

1. Select **Asha Rao** in the upper-right profile selector.
2. Open **Find partners**. The app finds **Dev Nair** because Asha teaches Java and wants Python, while Dev teaches Python and wants Java.
3. Point out the two explanation rows. Choose **Request full exchange**. The app creates one session for each teaching direction.

### 3. Accept, complete, and rate a session

1. Select **Dev Nair**, then open **Sessions** and accept the pending Python request.
2. Select **Asha Rao**, open **Sessions**, and mark that Python session completed.
3. Choose **Rate this session**, select a star rating, and add feedback. The change is saved, and Dev's average updates in the profile and recommendations.

### 4. Three-person chain

1. Select **Meera Nair** and open **Find partners**. There is no direct exchange, so the default search shows the complete cycle.
2. Walk through the arrows: Meera learns Python from Arjun; Arjun learns SQL from Kabir; Kabir learns Java from Meera.
3. Choose **Schedule full chain**. In **Sessions**, each teaching edge appears as its own pending session, linked to the same chain.

### 5. No-match and registration

- Select **Tara Joseph** and open **Find partners**. The app explains that no complete compatible exchange is available.
- In **Profile & join**, register a sample student with a unique email and comma-separated skills. The new profile appears in the selector and remains after restart.

## OOP explanation for the viva

- **Abstraction:** `Person` holds shared identity behavior and is abstract.
- **Inheritance:** `Student extends Person` and adds skill preferences.
- **Interface and polymorphism:** `MatchStrategy` defines the contract; the engine invokes direct and chain implementations through that interface.
- **Encapsulation:** model fields are private; `Session` methods validate allowed state transitions.

## If asked how matching works

Direct matching checks both exchange directions and requires each teacher to meet the other student's requested proficiency. If no direct match exists, the chain strategy searches for a closed cycle of three or four distinct students. It limits the search depth, checks every edge, and returns the skill each learner receives. Ratings rank valid results but never make an incompatible skill pair match.
