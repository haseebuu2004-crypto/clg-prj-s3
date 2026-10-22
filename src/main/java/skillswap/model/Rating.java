package skillswap.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public final class Rating implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String reviewerId;
    private final int stars;
    private final String feedback;
    private final String learned;
    private final LocalDateTime createdAt;

    public Rating(String reviewerId, int stars, String feedback, String learned) {
        if (reviewerId == null || reviewerId.isBlank()) throw new IllegalArgumentException("Reviewer is required.");
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("Choose a rating from 1 to 5.");
        this.reviewerId = reviewerId;
        this.stars = stars;
        this.feedback = feedback == null ? "" : feedback.trim();
        this.learned = learned == null ? "" : learned.trim();
        this.createdAt = LocalDateTime.now();
    }

    public String getReviewerId() { return reviewerId; }
    public int getStars() { return stars; }
    public String getFeedback() { return feedback; }
    public String getLearned() { return learned; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
