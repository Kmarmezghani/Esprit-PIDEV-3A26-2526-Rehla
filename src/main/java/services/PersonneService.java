package services;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import models.Personne;
import Enumeration.Role;
import Enumeration.StatutCompte;
import util.DBConnection;

public class PersonneService {

    private Connection conn;

    public PersonneService() {
        conn = DBConnection.getInstance().getConn();
    }

    // Ajouter une personne
    public void addPersonne(Personne p) {
        String sql = "INSERT INTO personne (nom, prenom, email, motDePasse, dateInscription, role, statutCompte) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getMotDePasse());
            ps.setDate(5, Date.valueOf(LocalDate.now()));
            ps.setString(6, p.getRole().name());
            ps.setString(7, p.getStatutCompte().name());

            ps.executeUpdate();

            // récupérer l'id généré
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                p.setId(rs.getInt(1));
            }

            System.out.println("Personne ajoutée avec ID = " + p.getId());

        } catch (SQLException e) {
            System.out.println("Erreur addPersonne : " + e.getMessage());
        }
    }

    // Lister toutes les personnes
    public List<Personne> getAllPersonnes() {
        List<Personne> personnes = new ArrayList<>();
        String sql = "SELECT * FROM personne";
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Personne p = new Personne();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setEmail(rs.getString("email"));
                p.setMotDePasse(rs.getString("motDePasse"));
                p.setDateInscription(rs.getDate("dateInscription").toLocalDate());
                p.setRole(Role.valueOf(rs.getString("role")));
                p.setStatutCompte(StatutCompte.valueOf(rs.getString("statutCompte")));
                personnes.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAllPersonnes : " + e.getMessage());
        }
        return personnes;
    }
}

