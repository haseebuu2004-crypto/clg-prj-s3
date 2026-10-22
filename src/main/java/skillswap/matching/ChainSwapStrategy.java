package skillswap.matching;

import skillswap.model.Skill;
import skillswap.model.Student;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Searches directed cycles: A points to a student who can teach A a wanted skill. */
public final class ChainSwapStrategy implements MatchStrategy {
    private final int maxParticipants;
    public ChainSwapStrategy(int maxParticipants) {
        if (maxParticipants < 3) throw new IllegalArgumentException("A chain must allow at least three students.");
        this.maxParticipants = maxParticipants;
    }

    @Override public List<MatchResult> findMatches(Student focus, Collection<Student> candidates) {
        Map<String, Student> students = new LinkedHashMap<>();
        for (Student student : candidates) students.put(student.getId(), student);
        students.put(focus.getId(), focus);
        List<MatchResult> results = new ArrayList<>();
        Set<String> seenCycles = new HashSet<>();
        List<Student> path = new ArrayList<>();
        List<Exchange> edges = new ArrayList<>();
        path.add(focus);
        search(focus, focus, students.values(), path, edges, results, seenCycles);
        return results;
    }

    private void search(Student start, Student current, Collection<Student> all,
                        List<Student> path, List<Exchange> edges,
                        List<MatchResult> results, Set<String> seenCycles) {
        for (Student next : all) {
            if (next.getId().equals(current.getId())) continue;
            if (next.getId().equals(start.getId())) {
                if (path.size() < 3) continue;
                for (Skill skill : compatibleSkills(next, current)) {
                    List<Exchange> cycleEdges = new ArrayList<>(edges);
                    cycleEdges.add(new Exchange(current.getId(), next.getId(), skill.getName()));
                    String signature = canonicalSignature(path, cycleEdges);
                    if (seenCycles.add(signature)) {
                        List<String> ids = path.stream().map(Student::getId).toList();
                        results.add(new MatchResult(MatchResult.Type.CHAIN, ids, cycleEdges));
                    }
                }
                continue;
            }
            if (path.size() >= maxParticipants || containsId(path, next.getId())) continue;
            for (Skill skill : compatibleSkills(next, current)) {
                path.add(next);
                edges.add(new Exchange(current.getId(), next.getId(), skill.getName()));
                search(start, next, all, path, edges, results, seenCycles);
                edges.remove(edges.size() - 1);
                path.remove(path.size() - 1);
            }
        }
    }

    private List<Skill> compatibleSkills(Student teacher, Student learner) {
        List<Skill> result = new ArrayList<>();
        for (Skill wanted : learner.getWantedSkills()) {
            Skill offered = teacher.getOffered(wanted.getName());
            if (offered != null && offered.getProficiency().meets(wanted.getProficiency())) result.add(offered);
        }
        return result;
    }

    private boolean containsId(List<Student> path, String id) {
        return path.stream().anyMatch(student -> student.getId().equals(id));
    }

    private String canonicalSignature(List<Student> path, List<Exchange> edges) {
        String best = null;
        for (int offset = 0; offset < path.size(); offset++) {
            StringBuilder value = new StringBuilder();
            for (int step = 0; step < path.size(); step++) {
                int edgeIndex = (offset + step) % path.size();
                Exchange edge = edges.get(edgeIndex);
                value.append(edge.getLearnerId()).append("->").append(edge.getTeacherId())
                        .append(':').append(edge.getSkillName().toLowerCase(java.util.Locale.ROOT)).append('|');
            }
            String candidate = value.toString();
            if (best == null || candidate.compareTo(best) < 0) best = candidate;
        }
        return best;
    }
}
