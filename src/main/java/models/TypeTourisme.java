package models;

public enum TypeTourisme {
    SEASIDE("Seaside"),
    DESERT("Desert"),
    MOUNTAIN("Mountain"),
    URBAN("Urban"),
    CULTURAL("Cultural");

    private final String label;

    TypeTourisme(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
