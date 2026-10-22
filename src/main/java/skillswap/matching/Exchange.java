package skillswap.matching;

/** One learning direction: the teacher provides a skill to the learner. */
public final class Exchange {
    private final String learnerId;
    private final String teacherId;
    private final String skillName;

    public Exchange(String learnerId, String teacherId, String skillName) {
        this.learnerId = learnerId;
        this.teacherId = teacherId;
        this.skillName = skillName;
    }
    public String getLearnerId() { return learnerId; }
    public String getTeacherId() { return teacherId; }
    public String getSkillName() { return skillName; }
}
