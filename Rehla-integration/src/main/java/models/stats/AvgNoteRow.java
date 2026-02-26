package models.stats;

public class AvgNoteRow {
    private int id;
    private String nom;
    private double avgNote;
    private int nbAvis;

    public AvgNoteRow(int id, String nom, double avgNote, int nbAvis) {
        this.id = id;
        this.nom = nom;
        this.avgNote = avgNote;
        this.nbAvis = nbAvis;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public double getAvgNote() { return avgNote; }
    public int getNbAvis() { return nbAvis; }
}
