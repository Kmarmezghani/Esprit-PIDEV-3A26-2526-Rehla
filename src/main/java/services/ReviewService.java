package services;

import models.Review;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewService implements IService<Review> {

    private final Connection conn;

    public ReviewService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Review review) {
        String SQL = "INSERT INTO avis (note, commentaire, dateAvis, activite_id, personne_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
            stmt.setInt(1, review.getNote());
            stmt.setString(2, review.getCommentaire());
            if (review.getDateAvis() != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(review.getDateAvis()));
            } else {
                // si DB a DEFAULT CURRENT_TIMESTAMP, tu peux aussi mettre null
                stmt.setTimestamp(3, null);
            }

            stmt.setInt(4, review.getActiviteId());
            stmt.setInt(5, review.getPersonneId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Review review) {
        String SQL = "UPDATE avis SET note = ?, commentaire = ?, dateAvis = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
            stmt.setInt(1, review.getNote());
            stmt.setString(2, review.getCommentaire());

            if (review.getDateAvis() != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(review.getDateAvis()));
            } else {
                stmt.setTimestamp(3, null);
            }

            stmt.setInt(4, review.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Review review) {
        String SQL = "DELETE FROM avis WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
            stmt.setInt(1, review.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Review> getAll() {
        List<Review> reviews = new ArrayList<>();
        String SQL = "SELECT r.id, r.activite_id, r.personne_id, CONCAT(p.nom,' ',p.prenom) AS userName, " +
                "r.commentaire, r.note, r.dateAvis " +
                "FROM avis r " +
                "JOIN personne p ON r.personne_id = p.id " +
                "ORDER BY r.dateAvis DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL)) {

            while (rs.next()) {
                Review review = new Review();
                review.setId(rs.getInt("id"));
                review.setActiviteId(rs.getInt("activite_id"));
                review.setPersonneId(rs.getInt("personne_id"));
                review.setUserName(rs.getString("userName"));
                review.setCommentaire(rs.getString("commentaire"));
                review.setNote(rs.getInt("note"));
                Timestamp ts = rs.getTimestamp("dateAvis");
                review.setDateAvis(ts != null ? ts.toLocalDateTime() : null);

                reviews.add(review);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reviews;
    }

    public List<Review> getReviewsByActiviteId(int activiteId) {
        List<Review> reviews = new ArrayList<>();
        String SQL = "SELECT r.id, r.activite_id, r.personne_id, CONCAT(p.nom,' ',p.prenom) AS userName, " +
                "r.commentaire, r.note, r.dateAvis " +
                "FROM avis r " +
                "JOIN personne p ON r.personne_id = p.id " +
                "WHERE r.activite_id = ? " +
                "ORDER BY r.dateAvis DESC";

        try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
            stmt.setInt(1, activiteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setId(rs.getInt("id"));
                    review.setActiviteId(rs.getInt("activite_id"));
                    review.setPersonneId(rs.getInt("personne_id"));
                    review.setUserName(rs.getString("userName"));
                    review.setCommentaire(rs.getString("commentaire"));
                    review.setNote(rs.getInt("note"));

                    Timestamp ts = rs.getTimestamp("dateAvis");
                    review.setDateAvis(ts != null ? ts.toLocalDateTime() : null);

                    reviews.add(review);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reviews;
    }

    public double getAverageNoteByActiviteId(int activiteId) {
        String sql = "SELECT AVG(note) AS avgNote FROM avis WHERE activite_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("avgNote");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void deleteByIdAndUser(int reviewId, int userId) {
        String sql = "DELETE FROM avis WHERE id = ? AND personne_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
