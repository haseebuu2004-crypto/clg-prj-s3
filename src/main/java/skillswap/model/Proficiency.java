package skillswap.model;

public enum Proficiency {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced");

    private final String label;
    Proficiency(String label) { this.label = label; }
    public String getLabel() { return label; }
    public boolean meets(Proficiency requested) { return ordinal() >= requested.ordinal(); }
    @Override public String toString() { return label; }
}
