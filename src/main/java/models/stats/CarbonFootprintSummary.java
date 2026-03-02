package models.stats;

/**
 * Simple DTO representing a user's carbon footprint for their trips.
 */
public record CarbonFootprintSummary(
        double totalKg,
        double averagePerReservationKg,
        int reservationsCount
) {}

