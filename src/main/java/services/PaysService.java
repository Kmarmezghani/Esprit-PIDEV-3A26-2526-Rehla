package services;

import models.Pays;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaysService implements IService<Pays> {
    Connection conn;

    public PaysService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Pays pays) {
        String SQL = "INSERT INTO pays (nom, description) VALUES (?, ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, pays.getNom());
            pstmt.setString(2, pays.getDescription());
            pstmt.executeUpdate();
            System.out.println("Pays added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Pays pays) {
        String SQL = "UPDATE pays SET nom = ?, description = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, pays.getNom());
            pstmt.setString(2, pays.getDescription());
            pstmt.setInt(3, pays.getId());
            pstmt.executeUpdate();
            System.out.println("Pays updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Pays pays) {
        String SQL = "DELETE FROM pays WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setInt(1, pays.getId());
            pstmt.executeUpdate();
            System.out.println("Pays deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Pays> getAll() {
        String SQL = "SELECT * FROM pays";
        List<Pays> paysList = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                Pays pays = new Pays();
                pays.setId(rs.getInt("id"));
                pays.setNom(rs.getString("nom"));
                pays.setDescription(rs.getString("description"));
                paysList.add(pays);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return paysList;
    }

    public Pays getById(int id) {
        String SQL = "SELECT * FROM pays WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Pays pays = new Pays();
                pays.setId(rs.getInt("id"));
                pays.setNom(rs.getString("nom"));
                pays.setDescription(rs.getString("description"));
                return pays;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
