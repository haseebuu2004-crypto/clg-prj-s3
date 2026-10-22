package skillswap.matching;

import skillswap.model.Student;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Runs direct matching first, then searches chains only when no direct result exists. */
public final class MatchEngine {
    private final MatchStrategy directStrategy = new DirectSwapStrategy();
    private final MatchStrategy chainStrategy = new ChainSwapStrategy(4);
    private final ToDoubleFunction<String> ratingForStudent;

    public MatchEngine(ToDoubleFunction<String> ratingForStudent) { this.ratingForStudent = ratingForStudent; }

    public List<MatchResult> findMatches(Student focus, Collection<Student> students) {
        List<MatchResult> direct = directStrategy.findMatches(focus, students);
        if (!direct.isEmpty()) {
            direct.sort(Comparator.<MatchResult>comparingInt(m -> m.getExchanges().size()).reversed()
                    .thenComparing(Comparator.comparingDouble(this::averagePartnerRating).reversed()));
            return direct;
        }
        return findChains(focus, students);
    }

    /** Explicit exploration is available to make a valid cycle easy to demonstrate. */
    public List<MatchResult> findChains(Student focus, Collection<Student> students) {
        List<MatchResult> chains = new ArrayList<>(chainStrategy.findMatches(focus, students));
        chains.sort(Comparator.<MatchResult>comparingInt(m -> m.getParticipantIds().size())
                .thenComparing(Comparator.comparingDouble(this::averagePartnerRating).reversed()));
        return chains;
    }

    private double averagePartnerRating(MatchResult result) {
        return result.getParticipantIds().stream().filter(id -> !id.equals(result.getParticipantIds().get(0)))
                .mapToDouble(ratingForStudent).average().orElse(3.0);
    }
}
