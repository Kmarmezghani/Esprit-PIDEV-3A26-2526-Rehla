package models;

public enum TypeAttraction {
    HISTORICAL("Historical"),
    NATURE("Nature"),
    ENTERTAINMENT("Entertainment"),
    RELIGIOUS("Religious"),
    ADVENTUROUS("Adventurous");

    private final String label;

    TypeAttraction(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}