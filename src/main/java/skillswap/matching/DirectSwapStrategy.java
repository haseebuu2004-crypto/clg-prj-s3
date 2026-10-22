package skillswap.matching;

import skillswap.model.Skill;
import skillswap.model.Student;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Finds reciprocal skill exchanges where both students receive a wanted skill. */
public final class DirectSwapStrategy implements MatchStrategy {
    @Override public List<MatchResult> findMatches(Student focus, Collection<Student> candidates) {
        List<MatchResult> results = new ArrayList<>();
        for (Student other : candidates) {
            if (focus.getId().equals(other.getId())) continue;
            List<Exchange> exchanges = new ArrayList<>();
            exchanges.addAll(offersFor(other, focus));
            exchanges.addAll(offersFor(focus, other));
            boolean focusLearns = exchanges.stream().anyMatch(e -> e.getLearnerId().equals(focus.getId()));
            boolean otherLearns = exchanges.stream().anyMatch(e -> e.getLearnerId().equals(other.getId()));
            if (focusLearns && otherLearns) {
                results.add(new MatchResult(MatchResult.Type.DIRECT, List.of(focus.getId(), other.getId()), exchanges));
            }
        }
        return results;
    }

    private List<Exchange> offersFor(Student teacher, Student learner) {
        List<Exchange> result = new ArrayList<>();
        for (Skill requested : learner.getWantedSkills()) {
            Skill offered = teacher.getOffered(requested.getName());
            if (offered != null && offered.getProficiency().meets(requested.getProficiency())) {
                result.add(new Exchange(learner.getId(), teacher.getId(), offered.getName()));
            }
        }
        return result;
    }
}
