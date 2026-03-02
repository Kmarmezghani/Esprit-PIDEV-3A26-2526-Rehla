package models.stats;

public record UserReputationSummary(
        int score,
        int totalReservations,
        int confirmedReservations,
        int cancelledReservations,
        int reviewsCount,
        String levelLabel,
        String levelColorHex
) {}