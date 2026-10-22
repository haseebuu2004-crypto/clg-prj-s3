package skillswap.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One teaching meeting. A chain is stored as one ordinary session per exchange edge. */
public final class Session implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Status { PENDING("Pending"), ACCEPTED("Accepted"), COMPLETED("Completed"), CANCELLED("Cancelled");
        private final String label;
        Status(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final String id;
    private final String teacherId;
    private final String learnerId;
    private final String skillName;
    private final String matchGroupId;
    private LocalDateTime scheduledAt;
    private String notes;
    private Status status;
    private LocalDateTime completedAt;
    private Rating rating;

    public Session(String id, String teacherId, String learnerId, String skillName,
                   LocalDateTime scheduledAt, String notes, String matchGroupId) {
        if (id == null || teacherId == null || learnerId == null || skillName == null) throw new IllegalArgumentException("Session details are incomplete.");
        if (teacherId.equals(learnerId)) throw new IllegalArgumentException("A student cannot book a session with themself.");
        this.id = id;
        this.teacherId = teacherId;
        this.learnerId = learnerId;
        this.skillName = skillName;
        this.scheduledAt = scheduledAt == null ? LocalDateTime.now().plusDays(1) : scheduledAt;
        this.notes = notes == null ? "" : notes.trim();
        this.matchGroupId = matchGroupId == null ? id : matchGroupId;
        this.status = Status.PENDING;
    }

    public String getId() { return id; }
    public String getTeacherId() { return teacherId; }
    public String getLearnerId() { return learnerId; }
    public String getSkillName() { return skillName; }
    public String getMatchGroupId() { return matchGroupId; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getNotes() { return notes; }
    public Status getStatus() { return status; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Rating getRating() { return rating; }

    public void accept() {
        requireStatus(Status.PENDING, "Only a pending session can be accepted.");
        status = Status.ACCEPTED;
    }
    public void complete() {
        requireStatus(Status.ACCEPTED, "Accept the session before marking it complete.");
        status = Status.COMPLETED;
        completedAt = LocalDateTime.now();
    }
    public void cancel() {
        if (status == Status.COMPLETED || status == Status.CANCELLED) throw new IllegalStateException("This session can no longer be cancelled.");
        status = Status.CANCELLED;
    }
    public void reschedule(LocalDateTime dateTime) {
        if (status != Status.PENDING && status != Status.ACCEPTED) throw new IllegalStateException("Only pending or accepted sessions can be rescheduled.");
        if (dateTime == null || !dateTime.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Choose a future date and time.");
        scheduledAt = dateTime;
    }
    public void addRating(Rating rating) {
        requireStatus(Status.COMPLETED, "Only a completed session can be rated.");
        if (this.rating != null) throw new IllegalStateException("This session already has feedback.");
        if (!learnerId.equals(rating.getReviewerId())) throw new IllegalArgumentException("Only the learner can rate this session.");
        this.rating = rating;
    }

    /** Used only to roll back an in-memory change if an atomic save fails. */
    public void restore(Status priorStatus, LocalDateTime priorCompletedAt, LocalDateTime priorSchedule, Rating priorRating) {
        status = priorStatus;
        completedAt = priorCompletedAt;
        scheduledAt = priorSchedule;
        rating = priorRating;
    }

    private void requireStatus(Status expected, String message) {
        if (status != expected) throw new IllegalStateException(message);
    }
}
