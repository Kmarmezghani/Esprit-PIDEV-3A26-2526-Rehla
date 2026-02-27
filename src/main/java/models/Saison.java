package models;

public enum Saison {
    WINTER("Winter"),
    SPRING("Spring"),
    SUMMER("Summer"),
    AUTUMN("Autumn"),
    ALL_YEAR("All Year");

    private final String label;

    Saison(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Saison fromString(String text) {
        for (Saison s : Saison.values()) {
            if (s.label.equalsIgnoreCase(text)) {
                return s;
            }
        }
        return null;
    }
}