package skillswap.matching;

import skillswap.model.Student;
import java.util.Collection;
import java.util.List;

/** Strategy contract keeps matching rules independent of the Swing interface. */
public interface MatchStrategy {
    List<MatchResult> findMatches(Student focus, Collection<Student> candidates);
}
