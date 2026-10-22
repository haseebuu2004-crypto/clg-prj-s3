package skillswap.matching;

import java.util.List;

public final class MatchResult {
    public enum Type { DIRECT, CHAIN }
    private final Type type;
    private final List<String> participantIds;
    private final List<Exchange> exchanges;

    public MatchResult(Type type, List<String> participantIds, List<Exchange> exchanges) {
        this.type = type;
        this.participantIds = List.copyOf(participantIds);
        this.exchanges = List.copyOf(exchanges);
    }
    public Type getType() { return type; }
    public List<String> getParticipantIds() { return participantIds; }
    public List<Exchange> getExchanges() { return exchanges; }
    public boolean includes(String studentId) { return participantIds.contains(studentId); }
}
