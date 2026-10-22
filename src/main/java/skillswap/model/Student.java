package skillswap.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A registered learner who can teach some skills and request help with others. */
public final class Student extends Person {
    private static final long serialVersionUID = 1L;

    private final Map<String, Skill> offeredSkills = new LinkedHashMap<>();
    private final Map<String, Skill> wantedSkills = new LinkedHashMap<>();

    public Student(String id, String name, String email) { super(id, name, email); }

    public void addSkill(Skill skill) {
        Map<String, Skill> target = skill.getDirection() == Skill.Direction.OFFERED ? offeredSkills : wantedSkills;
        if (target.containsKey(skill.key())) throw new IllegalArgumentException("That skill is already in this list.");
        target.put(skill.key(), skill);
    }

    public Skill removeSkill(Skill.Direction direction, String name) {
        return map(direction).remove(name.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public Collection<Skill> getOfferedSkills() { return List.copyOf(offeredSkills.values()); }
    public Collection<Skill> getWantedSkills() { return List.copyOf(wantedSkills.values()); }
    public boolean offers(String skillName) { return offeredSkills.containsKey(skillName.trim().toLowerCase(java.util.Locale.ROOT)); }
    public boolean wants(String skillName) { return wantedSkills.containsKey(skillName.trim().toLowerCase(java.util.Locale.ROOT)); }
    public Skill getOffered(String skillName) { return offeredSkills.get(skillName.trim().toLowerCase(java.util.Locale.ROOT)); }
    public Skill getWanted(String skillName) { return wantedSkills.get(skillName.trim().toLowerCase(java.util.Locale.ROOT)); }
    public List<Skill> offeredAsList() { return new ArrayList<>(offeredSkills.values()); }
    public List<Skill> wantedAsList() { return new ArrayList<>(wantedSkills.values()); }

    private Map<String, Skill> map(Skill.Direction direction) {
        return direction == Skill.Direction.OFFERED ? offeredSkills : wantedSkills;
    }
}
