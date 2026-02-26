package models;

/**
 * Entity matching the database table `preference`.
 * Links to a user via personne_id.
 */
public class Preference {

    private int id;
    private Double budgetMin;
    private Double budgetMax;
    private String typesVoyage;
    private String centresInteret;
    private int personneId;

    public Preference() {}

    public Preference(int id, Double budgetMin, Double budgetMax, String typesVoyage,
                      String centresInteret, int personneId) {
        this.id = id;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.typesVoyage = typesVoyage;
        this.centresInteret = centresInteret;
        this.personneId = personneId;
    }

    public Preference(Double budgetMin, Double budgetMax, String typesVoyage,
                      String centresInteret, int personneId) {
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.typesVoyage = typesVoyage;
        this.centresInteret = centresInteret;
        this.personneId = personneId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Double getBudgetMin() { return budgetMin; }
    public void setBudgetMin(Double budgetMin) { this.budgetMin = budgetMin; }

    public Double getBudgetMax() { return budgetMax; }
    public void setBudgetMax(Double budgetMax) { this.budgetMax = budgetMax; }

    public String getTypesVoyage() { return typesVoyage; }
    public void setTypesVoyage(String typesVoyage) { this.typesVoyage = typesVoyage; }

    public String getCentresInteret() { return centresInteret; }
    public void setCentresInteret(String centresInteret) { this.centresInteret = centresInteret; }

    public int getPersonneId() { return personneId; }
    public void setPersonneId(int personneId) { this.personneId = personneId; }
}
