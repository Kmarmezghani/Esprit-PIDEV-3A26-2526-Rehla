package models;

import Enumeration.Role;
import Enumeration.StatutCompte;

public class Guide extends Personne {

    private String specialite;
    private String langues;
    private int experience;

    public Guide() {
        this.role = Role.GUIDE;
    }

    public Guide(int id, String nom, String prenom, String email,
                 String motDePasse, String specialite,
                 String langues, int experience) {

        super(id, nom, prenom, email, motDePasse, null,
                Role.GUIDE, StatutCompte.ACTIF, null);

        this.specialite = specialite;
        this.langues = langues;
        this.experience = experience;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getLangues() {
        return langues;
    }

    public void setLangues(String langues) {
        this.langues = langues;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }


}

