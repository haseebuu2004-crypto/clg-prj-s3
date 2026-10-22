package skillswap.service;

import skillswap.matching.Exchange;
import skillswap.matching.MatchEngine;
import skillswap.matching.MatchResult;
import skillswap.model.Proficiency;
import skillswap.model.Rating;
import skillswap.model.Session;
import skillswap.model.Skill;
import skillswap.model.Student;
import skillswap.persistence.LocalStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Application services and validation; Swing screens call this class instead of storing UI state. */
public final class SkillSwapService {
    private final LocalStore store;
    private final Map<String, Student> students = new LinkedHashMap<>();
    private final List<Session> sessions = new ArrayList<>();
    private final MatchEngine matchEngine = new MatchEngine(this::ratingScore);

    public SkillSwapService() throws IOException {
        this(Path.of(System.getProperty("user.home"), ".skillswap", "skillswap-data.bin"));
    }

    public SkillSwapService(Path dataFile) throws IOException {
        store = new LocalStore(dataFile);
        LocalStore.Snapshot snapshot = store.load();
        students.putAll(snapshot.getStudents());
        sessions.addAll(snapshot.getSessions());
        if (students.isEmpty()) loadDemoData();
    }

    public Path getDataFile() { return store.getFile(); }
    public Collection<Student> getStudents() { return List.copyOf(students.values()); }
    public Student getStudent(String id) {
        Student student = students.get(id);
        if (student == null) throw new IllegalArgumentException("That student profile is no longer available.");
        return student;
    }
    public List<Session> getSessions() {
        return sessions.stream().sorted(Comparator.comparing(Session::getScheduledAt).reversed()).toList();
    }
    public List<Session> getSessionsFor(String studentId) {
        return sessions.stream().filter(s -> s.getTeacherId().equals(studentId) || s.getLearnerId().equals(studentId))
                .sorted(Comparator.comparing(Session::getScheduledAt).reversed()).toList();
    }
    public List<MatchResult> findMatches(String studentId) { return matchEngine.findMatches(getStudent(studentId), students.values()); }
    public List<MatchResult> findChainMatches(String studentId) { return matchEngine.findChains(getStudent(studentId), students.values()); }

    public Student register(String name, String email, String offered, String wanted,
                            Proficiency teachingLevel, Proficiency learningLevel) throws IOException {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Enter the student's name.");
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!cleanEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) throw new IllegalArgumentException("Enter a valid email address.");
        if (students.values().stream().anyMatch(s -> s.getEmail().equalsIgnoreCase(cleanEmail))) throw new IllegalArgumentException("A profile with that email already exists.");
        List<String> offeredNames = parseSkills(offered);
        List<String> wantedNames = parseSkills(wanted);
        if (offeredNames.isEmpty() || wantedNames.isEmpty()) throw new IllegalArgumentException("Add at least one skill to teach and one to learn.");
        Student student = new Student(UUID.randomUUID().toString(), name, cleanEmail);
        for (String skill : offeredNames) student.addSkill(new Skill(skill, "General", teachingLevel, Skill.Direction.OFFERED));
        for (String skill : wantedNames) student.addSkill(new Skill(skill, "General", learningLevel, Skill.Direction.WANTED));
        students.put(student.getId(), student);
        try { save(); } catch (IOException ex) { students.remove(student.getId()); throw ex; }
        return student;
    }

    public void addSkill(String studentId, Skill.Direction direction, String name, String category, Proficiency level) throws IOException {
        Student student = getStudent(studentId);
        Skill skill = new Skill(name, category, level, direction);
        student.addSkill(skill);
        try { save(); } catch (IOException ex) { student.removeSkill(direction, skill.getName()); throw ex; }
    }

    public void updateSkill(String studentId, Skill.Direction direction, String oldName, String newName,
                            String category, Proficiency level) throws IOException {
        Student student = getStudent(studentId);
        Skill updated = new Skill(newName, category, level, direction);
        Skill original = student.removeSkill(direction, oldName);
        if (original == null) throw new IllegalArgumentException("That skill is no longer in this list.");
        try {
            student.addSkill(updated);
        } catch (IllegalArgumentException ex) {
            student.addSkill(original);
            throw ex;
        }
        try { save(); }
        catch (IOException ex) {
            student.removeSkill(direction, updated.getName());
            student.addSkill(original);
            throw ex;
        }
    }

    public void removeSkill(String studentId, Skill.Direction direction, String name) throws IOException {
        Student student = getStudent(studentId);
        Skill removed = student.removeSkill(direction, name);
        if (removed == null) return;
        try { save(); } catch (IOException ex) { student.addSkill(removed); throw ex; }
    }

    public void updateProfile(String studentId, String name, String email) throws IOException {
        Student student = getStudent(studentId);
        String originalName = student.getName();
        String originalEmail = student.getEmail();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Enter the student's name.");
        if (!cleanEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) throw new IllegalArgumentException("Enter a valid email address.");
        if (students.values().stream().anyMatch(s -> !s.getId().equals(studentId) && s.getEmail().equalsIgnoreCase(cleanEmail))) {
            throw new IllegalArgumentException("A profile with that email already exists.");
        }
        student.setName(name);
        student.setEmail(cleanEmail);
        try { save(); }
        catch (IOException ex) { student.setName(originalName); student.setEmail(originalEmail); throw ex; }
    }

    public List<Session> scheduleDirectExchange(String currentStudentId, MatchResult result) throws IOException {
        requireResult(result, MatchResult.Type.DIRECT, currentStudentId);
        String groupId = UUID.randomUUID().toString();
        List<Session> created = new ArrayList<>();
        int offset = 0;
        for (Exchange exchange : result.getExchanges()) {
            validateExchange(exchange);
            created.add(new Session(UUID.randomUUID().toString(), exchange.getTeacherId(), exchange.getLearnerId(),
                    exchange.getSkillName(), LocalDateTime.now().plusDays(1).plusHours(offset++),
                    "Direct skill exchange", groupId));
        }
        return commitSessions(created);
    }

    public List<Session> scheduleChain(MatchResult result, String currentStudentId) throws IOException {
        requireResult(result, MatchResult.Type.CHAIN, currentStudentId);
        String groupId = UUID.randomUUID().toString();
        List<Session> created = new ArrayList<>();
        int offset = 0;
        for (Exchange exchange : result.getExchanges()) {
            validateExchange(exchange);
            created.add(new Session(UUID.randomUUID().toString(), exchange.getTeacherId(), exchange.getLearnerId(),
                    exchange.getSkillName(), LocalDateTime.now().plusDays(1).plusHours(offset++),
                    "Chain exchange · each participant receives one requested skill", groupId));
        }
        return commitSessions(created);
    }

    public void acceptSession(String sessionId, String currentStudentId) throws IOException {
        Session session = getSession(sessionId);
        if (!session.getTeacherId().equals(currentStudentId)) throw new IllegalArgumentException("Switch to the teaching student to accept this request.");
        updateSession(session, session::accept);
    }
    public void completeSession(String sessionId, String currentStudentId) throws IOException {
        Session session = getSession(sessionId);
        requireParticipant(session, currentStudentId);
        updateSession(session, session::complete);
    }
    public void cancelSession(String sessionId, String currentStudentId) throws IOException {
        Session session = getSession(sessionId);
        requireParticipant(session, currentStudentId);
        updateSession(session, session::cancel);
    }
    public void rescheduleSession(String sessionId, String currentStudentId, LocalDateTime dateTime) throws IOException {
        Session session = getSession(sessionId);
        requireParticipant(session, currentStudentId);
        updateSession(session, () -> session.reschedule(dateTime));
    }
    public void rateSession(String sessionId, String reviewerId, int stars, String feedback, String learned) throws IOException {
        Session session = getSession(sessionId);
        Rating rating = new Rating(reviewerId, stars, feedback, learned);
        updateSession(session, () -> session.addRating(rating));
    }

    public double getAverageRating(String studentId) {
        return sessions.stream().filter(s -> s.getTeacherId().equals(studentId) && s.getRating() != null)
                .mapToInt(s -> s.getRating().getStars()).average().orElse(0.0);
    }
    public int getRatingCount(String studentId) {
        return (int) sessions.stream().filter(s -> s.getTeacherId().equals(studentId) && s.getRating() != null).count();
    }
    public List<Session> getRecentFeedback(String studentId, int limit) {
        return sessions.stream().filter(s -> s.getTeacherId().equals(studentId) && s.getRating() != null)
                .sorted(Comparator.comparing(s -> s.getRating().getCreatedAt(), Comparator.reverseOrder())).limit(limit).toList();
    }
    public long countUpcoming(String studentId) {
        return sessions.stream().filter(s -> (s.getTeacherId().equals(studentId) || s.getLearnerId().equals(studentId))
                && (s.getStatus() == Session.Status.PENDING || s.getStatus() == Session.Status.ACCEPTED)).count();
    }
    public long countCompleted(String studentId) {
        return sessions.stream().filter(s -> (s.getTeacherId().equals(studentId) || s.getLearnerId().equals(studentId))
                && s.getStatus() == Session.Status.COMPLETED).count();
    }

    /** Adds only demo rows whose identifiers/emails are not already present; existing records are never replaced. */
    public void loadDemoData() throws IOException {
        boolean changed = false;
        boolean existed = hasEmail("asha@skillswap.demo");
        Student asha = ensureDemoStudent("Asha Rao", "asha@skillswap.demo", skills("Java:ADVANCED"), skills("Python:INTERMEDIATE")); changed |= !existed;
        existed = hasEmail("dev@skillswap.demo");
        Student dev = ensureDemoStudent("Dev Nair", "dev@skillswap.demo", skills("Python:ADVANCED"), skills("Java:ADVANCED")); changed |= !existed;
        existed = hasEmail("meera@skillswap.demo");
        Student meera = ensureDemoStudent("Meera Nair", "meera@skillswap.demo", skills("Java:INTERMEDIATE"), skills("Python:INTERMEDIATE")); changed |= !existed;
        existed = hasEmail("arjun@skillswap.demo");
        Student arjun = ensureDemoStudent("Arjun Menon", "arjun@skillswap.demo", skills("Python:ADVANCED"), skills("SQL:INTERMEDIATE")); changed |= !existed;
        existed = hasEmail("kabir@skillswap.demo");
        Student kabir = ensureDemoStudent("Kabir Shah", "kabir@skillswap.demo", skills("SQL:ADVANCED"), skills("Java:INTERMEDIATE")); changed |= !existed;
        existed = hasEmail("tara@skillswap.demo");
        Student tara = ensureDemoStudent("Tara Joseph", "tara@skillswap.demo", skills("French:ADVANCED"), skills("Graphic Design:BEGINNER")); changed |= !existed;
        changed |= ensureSeededSession("demo-history-dev-asha", dev, asha, "Python", 5, "Clear examples and patient explanations.", "Python basics and functions.");
        changed |= ensureSeededSession("demo-history-asha-dev", asha, dev, "Java", 4, "The object-oriented examples helped a lot.", "Classes and constructors.");
        if (changed) save();
    }

    private Student ensureDemoStudent(String name, String email, List<String> offered, List<String> wanted) {
        Student existing = students.values().stream().filter(s -> s.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
        if (existing != null) return existing;
        String id = stableId(email);
        Student student = new Student(id, name, email);
        offered.forEach(value -> addSeedSkill(student, value, Skill.Direction.OFFERED));
        wanted.forEach(value -> addSeedSkill(student, value, Skill.Direction.WANTED));
        students.put(id, student);
        return student;
    }

    private boolean hasEmail(String email) {
        return students.values().stream().anyMatch(s -> s.getEmail().equalsIgnoreCase(email));
    }

    private boolean ensureSeededSession(String key, Student teacher, Student learner, String skill, int stars, String feedback, String learned) {
        String id = stableId(key);
        if (sessions.stream().anyMatch(s -> s.getId().equals(id))) return false;
        Session session = new Session(id, teacher.getId(), learner.getId(), skill,
                LocalDateTime.now().minusDays(7), "Seeded demo history", "demo-history");
        session.accept();
        session.complete();
        session.addRating(new Rating(learner.getId(), stars, feedback, learned));
        sessions.add(session);
        return true;
    }

    private void addSeedSkill(Student student, String encoded) {
        addSeedSkill(student, encoded, null);
    }
    private void addSeedSkill(Student student, String encoded, Skill.Direction direction) {
        String[] parts = encoded.split(":", 2);
        Skill.Direction actualDirection = direction != null ? direction : Skill.Direction.OFFERED;
        student.addSkill(new Skill(parts[0], "General", Proficiency.valueOf(parts[1]), actualDirection));
    }

    private List<String> skills(String... values) { return List.of(values); }
    private String stableId(String value) { return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString(); }
    private List<Session> commitSessions(List<Session> created) throws IOException {
        sessions.addAll(created);
        try { save(); } catch (IOException ex) { sessions.removeAll(created); throw ex; }
        return List.copyOf(created);
    }
    private void validateExchange(Exchange exchange) {
        Student teacher = getStudent(exchange.getTeacherId());
        Student learner = getStudent(exchange.getLearnerId());
        Skill offered = teacher.getOffered(exchange.getSkillName());
        Skill wanted = learner.getWanted(exchange.getSkillName());
        if (offered == null || wanted == null || !offered.getProficiency().meets(wanted.getProficiency())) {
            throw new IllegalArgumentException("This match is no longer compatible. Run Find Matches again.");
        }
    }
    private void requireResult(MatchResult result, MatchResult.Type type, String currentStudentId) {
        if (result == null || result.getType() != type || !result.includes(currentStudentId)) throw new IllegalArgumentException("Choose a current match result first.");
    }
    private Session getSession(String id) {
        return sessions.stream().filter(s -> s.getId().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("That session was not found."));
    }
    private void requireParticipant(Session session, String studentId) {
        if (!session.getTeacherId().equals(studentId) && !session.getLearnerId().equals(studentId)) throw new IllegalArgumentException("Only a participant can update this session.");
    }
    private void updateSession(Session session, Runnable mutation) throws IOException {
        Session.Status oldStatus = session.getStatus();
        LocalDateTime oldCompletedAt = session.getCompletedAt();
        LocalDateTime oldSchedule = session.getScheduledAt();
        Rating oldRating = session.getRating();
        mutation.run();
        try { save(); } catch (IOException ex) { session.restore(oldStatus, oldCompletedAt, oldSchedule, oldRating); throw ex; }
    }
    private double ratingScore(String studentId) {
        int count = getRatingCount(studentId);
        return count == 0 ? 3.0 : getAverageRating(studentId);
    }
    private List<String> parseSkills(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim).filter(part -> !part.isEmpty()).distinct()
                .collect(Collectors.toList());
    }
    private void save() throws IOException { store.save(new LocalStore.Snapshot(students, sessions)); }
}
