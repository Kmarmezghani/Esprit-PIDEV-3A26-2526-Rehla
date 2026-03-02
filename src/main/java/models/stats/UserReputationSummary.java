package models.stats;

/**
 * Aggregated view of a user's reputation score.
 */
public record UserReputationSummary(
        int score,
        int totalReservations,
        int confirmedReservations,
        int cancelledReservations,
        int reviewsCount,
        String levelLabel,
        String levelColorHex
) {}

