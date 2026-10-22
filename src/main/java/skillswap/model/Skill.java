package skillswap.model;

import java.io.Serializable;
import java.util.Locale;

/** A student's offered or requested skill and the proficiency level involved. */
public final class Skill implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Direction { OFFERED, WANTED }

    private final String name;
    private final String category;
    private final Proficiency proficiency;
    private final Direction direction;

    public Skill(String name, String category, Proficiency proficiency, Direction direction) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Enter a skill name.");
        if (proficiency == null || direction == null) throw new IllegalArgumentException("Choose a proficiency level.");
        this.name = name.trim();
        this.category = category == null || category.isBlank() ? "General" : category.trim();
        this.proficiency = proficiency;
        this.direction = direction;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public Proficiency getProficiency() { return proficiency; }
    public Direction getDirection() { return direction; }
    public String key() { return name.trim().toLowerCase(Locale.ROOT); }
    @Override public String toString() { return name + "  ·  " + proficiency.getLabel(); }
}
