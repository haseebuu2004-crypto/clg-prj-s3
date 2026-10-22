package skillswap;

import skillswap.matching.ChainSwapStrategy;
import skillswap.matching.DirectSwapStrategy;
import skillswap.matching.MatchEngine;
import skillswap.matching.MatchResult;
import skillswap.model.Proficiency;
import skillswap.model.Session;
import skillswap.model.Skill;
import skillswap.model.Student;
import skillswap.service.SkillSwapService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-free acceptance checks for the project demo's key workflows. */
public final class SkillSwapSelfTest {
    private static int checks;
    private SkillSwapSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("skillswap-check-");
        Path dataFile = directory.resolve("demo.bin");
        SkillSwapService service = new SkillSwapService(dataFile);
        check(service.getStudents().size() == 6, "First launch seeds six demo profiles");

        Student asha = byEmail(service, "asha@skillswap.demo");
        Student dev = byEmail(service, "dev@skillswap.demo");
        Student meera = byEmail(service, "meera@skillswap.demo");
        Student tara = byEmail(service, "tara@skillswap.demo");

        List<MatchResult> direct = service.findMatches(asha.getId());
        check(direct.size() == 1 && direct.get(0).getType() == MatchResult.Type.DIRECT, "Asha receives the valid direct match first");
        check(direct.get(0).getParticipantIds().contains(dev.getId()), "Direct match uses reciprocal skill compatibility");
        check(direct.get(0).getExchanges().size() == 2, "Direct match explains both directions of the exchange");
        check(service.findMatches(tara.getId()).isEmpty(), "No-match profile returns an empty result safely");

        List<MatchResult> chainResults = service.findMatches(meera.getId());
        check(chainResults.size() == 1 && chainResults.get(0).getType() == MatchResult.Type.CHAIN, "No direct match falls back to a complete chain");
        MatchResult chain = chainResults.get(0);
        check(chain.getParticipantIds().size() == 3, "Seed chain includes three distinct students");
        check(chain.getExchanges().size() == 3, "Every participant receives a wanted skill");
        check(chain.getParticipantIds().stream().distinct().count() == 3, "Chain does not repeat a participant");
        check(chain.getExchanges().stream().allMatch(exchange -> chain.getParticipantIds().contains(exchange.getLearnerId()) && chain.getParticipantIds().contains(exchange.getTeacherId())), "Every chain edge stays inside the cycle");

        Student weakTeacher = student("weak", "Weak Teacher", "weak@example.edu", "Python", Proficiency.BEGINNER, "Java", Proficiency.INTERMEDIATE);
        Student advancedLearner = student("advanced", "Advanced Learner", "advanced@example.edu", "Java", Proficiency.ADVANCED, "Python", Proficiency.ADVANCED);
        check(new DirectSwapStrategy().findMatches(advancedLearner, List.of(weakTeacher, advancedLearner)).isEmpty(), "Proficiency below requested level blocks a direct match");

        Student rankFocus = student("rank-focus", "Rank Focus", "focus@example.edu", "Kotlin", Proficiency.ADVANCED, "SQL", Proficiency.INTERMEDIATE);
        Student lowRated = student("low-rated", "Low Rated", "low@example.edu", "SQL", Proficiency.ADVANCED, "Kotlin", Proficiency.INTERMEDIATE);
        Student highRated = student("high-rated", "High Rated", "high@example.edu", "SQL", Proficiency.ADVANCED, "Kotlin", Proficiency.INTERMEDIATE);
        List<MatchResult> ranked = new MatchEngine(id -> id.equals("high-rated") ? 4.9 : 2.0).findMatches(rankFocus, List.of(rankFocus, lowRated, highRated));
        check(ranked.get(0).getParticipantIds().contains("high-rated"), "Ratings rank only otherwise-compatible direct matches");

        Student cycleA = student("cycle-a", "Cycle A", "a@example.edu", "Java", Proficiency.ADVANCED, "Python", Proficiency.BEGINNER);
        Student cycleB = student("cycle-b", "Cycle B", "b@example.edu", "Python", Proficiency.ADVANCED, "SQL", Proficiency.BEGINNER);
        Student cycleC = student("cycle-c", "Cycle C", "c@example.edu", "SQL", Proficiency.ADVANCED, "Java", Proficiency.BEGINNER);
        List<MatchResult> validCycles = new ChainSwapStrategy(5).findMatches(cycleA, List.of(cycleA, cycleB, cycleC));
        check(validCycles.size() == 1, "Three-person cycle is found once");
        Student deadEnd = student("dead", "Dead End", "dead@example.edu", "Nope", Proficiency.ADVANCED, "Nowhere", Proficiency.BEGINNER);
        check(new ChainSwapStrategy(5).findMatches(cycleA, List.of(cycleA, cycleB, deadEnd)).isEmpty(), "Invalid/incomplete chain is rejected");

        List<Session> directSessions = service.scheduleDirectExchange(asha.getId(), direct.get(0));
        check(directSessions.size() == 2, "Direct exchange creates a separate session for each teaching direction");
        Session pythonSession = directSessions.stream().filter(s -> s.getTeacherId().equals(dev.getId())).findFirst().orElseThrow();
        expectFailure(() -> service.completeSession(pythonSession.getId(), asha.getId()), "Pending session cannot be completed");
        expectFailure(() -> service.rateSession(pythonSession.getId(), asha.getId(), 5, "Too soon", ""), "Incomplete session cannot be rated");
        expectFailure(() -> service.acceptSession(pythonSession.getId(), asha.getId()), "Only the teacher can accept");
        service.acceptSession(pythonSession.getId(), dev.getId());
        check(pythonSession.getStatus() == Session.Status.ACCEPTED, "Teacher can accept a pending request");
        service.completeSession(pythonSession.getId(), asha.getId());
        service.rateSession(pythonSession.getId(), asha.getId(), 4, "Clear explanations.", "Python functions");
        check(service.getRatingCount(dev.getId()) == 2, "Saved feedback updates teaching rating count");
        check(Math.abs(service.getAverageRating(dev.getId()) - 4.5) < 0.001, "Saved rating changes average reputation");
        expectFailure(() -> service.rateSession(pythonSession.getId(), asha.getId(), 5, "Duplicate", ""), "Duplicate rating is rejected");
        expectFailure(() -> service.cancelSession(pythonSession.getId(), asha.getId()), "Completed session cannot be cancelled");

        Session javaSession = directSessions.stream().filter(s -> s.getTeacherId().equals(asha.getId())).findFirst().orElseThrow();
        service.cancelSession(javaSession.getId(), asha.getId());
        expectFailure(() -> service.completeSession(javaSession.getId(), dev.getId()), "Cancelled session cannot be completed");

        List<Session> chainSessions = service.scheduleChain(chain, meera.getId());
        check(chainSessions.size() == 3, "Scheduling a chain creates one session per teaching edge");
        check(chainSessions.stream().map(Session::getMatchGroupId).distinct().count() == 1, "Chain sessions remain grouped as one exchange");

        Student registered = service.register("Nina Student", "nina@example.edu", "Kotlin", "UX design", Proficiency.ADVANCED, Proficiency.BEGINNER);
        check(service.getStudent(registered.getId()).getOfferedSkills().size() == 1, "Registration saves offered skills");
        expectFailure(() -> service.register("Other Student", "NINA@example.edu", "Python", "SQL", Proficiency.BEGINNER, Proficiency.BEGINNER), "Duplicate email is rejected");
        expectFailure(() -> service.addSkill(asha.getId(), Skill.Direction.OFFERED, "Java", "Technology", Proficiency.ADVANCED), "Duplicate skill is rejected");
        service.updateSkill(registered.getId(), Skill.Direction.OFFERED, "Kotlin", "Kotlin", "Technology", Proficiency.INTERMEDIATE);
        check(service.getStudent(registered.getId()).getOfferedSkills().iterator().next().getProficiency() == Proficiency.INTERMEDIATE, "Skill proficiency can be updated");
        service.addSkill(registered.getId(), Skill.Direction.OFFERED, "SQL", "Data", Proficiency.ADVANCED);
        expectFailure(() -> service.updateSkill(registered.getId(), Skill.Direction.OFFERED, "Kotlin", "SQL", "Data", Proficiency.BEGINNER), "Duplicate skill edits are rejected");
        check(service.getStudent(registered.getId()).offers("Kotlin") && service.getStudent(registered.getId()).offers("SQL"), "Failed skill edit restores the original skill");
        service.updateProfile(registered.getId(), "Nina Updated", "nina@example.edu");
        check(service.getStudent(registered.getId()).getName().equals("Nina Updated"), "Profile details can be updated");
        expectFailure(() -> service.updateProfile(registered.getId(), "Nina Updated", asha.getEmail()), "Profile edits reject another student's email");

        int studentCount = service.getStudents().size();
        int sessionCount = service.getSessions().size();
        service.addSkill(asha.getId(), Skill.Direction.OFFERED, "Public speaking", "Communication", Proficiency.INTERMEDIATE);
        service.loadDemoData();
        check(service.getStudents().size() == studentCount && service.getSessions().size() == sessionCount, "Repeat demo initialization does not duplicate records");
        check(service.getStudent(asha.getId()).offers("Public speaking"), "Demo initialization preserves saved profile changes");

        SkillSwapService reloaded = new SkillSwapService(dataFile);
        check(reloaded.getStudents().size() == studentCount, "Profiles survive closing and reopening the service");
        check(reloaded.getSessions().size() == sessionCount, "Sessions and ratings survive closing and reopening the service");
        check(reloaded.getStudent(registered.getId()).getName().equals("Nina Updated"), "Profile edits survive reload");
        check(reloaded.getStudent(registered.getId()).getOffered("Kotlin").getProficiency() == Proficiency.INTERMEDIATE, "Skill edits survive reload");
        check(Math.abs(reloaded.getAverageRating(dev.getId()) - 4.5) < 0.001, "Rating summary survives reload");
        check(reloaded.findMatches(asha.getId()).get(0).getType() == MatchResult.Type.DIRECT, "Matching remains available after reload");

        System.out.println("PASS: " + checks + " Skill Swap acceptance checks");
    }

    private static Student byEmail(SkillSwapService service, String email) {
        return service.getStudents().stream().filter(s -> s.getEmail().equals(email)).findFirst().orElseThrow();
    }
    private static Student student(String id, String name, String email, String offered, Proficiency offerLevel, String wanted, Proficiency wantLevel) {
        Student student = new Student(id, name, email);
        student.addSkill(new Skill(offered, "Test", offerLevel, Skill.Direction.OFFERED));
        student.addSkill(new Skill(wanted, "Test", wantLevel, Skill.Direction.WANTED));
        return student;
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void expectFailure(ThrowingRunnable action, String message) throws Exception {
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError(message);
    }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}
