package services;

import entities.Reclamation;
import utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceReclamations implements IService<Reclamation> {
    private Connection con;

    public ServiceReclamations() {
        con = MyConnection.getInstance().getCnx();
        verifyTableExists();
    }

    private void verifyTableExists() {
        try {
            DatabaseMetaData metaData = con.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "reclamations", null);
            
            if (!tables.next()) {
                System.out.println("Création de la table reclamations...");
                createReclamationsTable();
            } else {
                System.out.println("La table reclamations existe déjà");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createReclamationsTable() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS reclamations (
                id_reclamation INT AUTO_INCREMENT PRIMARY KEY,
                titre VARCHAR(100) NOT NULL,
                contenu TEXT NOT NULL,
                date_creation DATE NOT NULL,
                etat VARCHAR(50) NOT NULL,
                date_traitement DATE,
                id_user INT NOT NULL,
                id_citizen INT NULL,
                private_message TEXT,
                FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE
            )
        """;
        
        try {
            // Try to add the date_traitement column if it doesn't exist
            String addColumnSQL = "ALTER TABLE reclamations ADD COLUMN IF NOT EXISTS date_traitement DATE";
            try (Statement stmt = con.createStatement()) {
                stmt.execute(addColumnSQL);
                System.out.println("Colonne date_traitement ajoutée avec succès");
            }
            
            // Create the table if it doesn't exist
            try (Statement stmt = con.createStatement()) {
                stmt.execute(createTableSQL);
                System.out.println("Table reclamations créée avec succès");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean verifyUserExists(int userId) {
        String query = "SELECT COUNT(*) FROM users WHERE id_user = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupère la liste de toutes les réclamations
     * @return Liste des réclamations
     */
    public List<Reclamation> afficher() throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();
        String query = "SELECT * FROM reclamations ORDER BY date_creation DESC";
        
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Reclamation r = new Reclamation(
                    rs.getInt("id_reclamation"),
                    rs.getString("titre"),
                    rs.getString("contenu"),
                    rs.getDate("date_creation").toLocalDate(),
                    rs.getDate("date_traitement") != null ? rs.getDate("date_traitement").toLocalDate() : null,
                    rs.getInt("id_user"),
                    rs.getString("etat"),
                    rs.getInt("id_citizen"),
                    rs.getString("private_message")
                );
                reclamations.add(r);
            }
        }
        
        return reclamations;
    }

    public void ajouter(Reclamation reclamation) throws SQLException {
        // Only check the user ID, not the citizen ID
        if (!verifyUserExists(reclamation.getIdUser())) {
            throw new SQLException("L'utilisateur avec l'ID " + reclamation.getIdUser() + " n'existe pas dans la base de données.");
        }

        System.out.println("Ajout d'une réclamation: " + reclamation);
        String req = "INSERT INTO reclamations(titre, contenu, date_creation, etat, id_user, id_citizen, private_message) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reclamation.getTitre());
            ps.setString(2, reclamation.getContenu());
            ps.setDate(3, java.sql.Date.valueOf(reclamation.getDateCreation()));
            ps.setString(4, reclamation.getEtat());
            ps.setInt(5, reclamation.getIdUser());

            // Handle idCitizen - set to NULL if 0
            if (reclamation.getIdCitizen() == 0) {
                ps.setInt(6, reclamation.getIdUser());
            } else {
                ps.setInt(6, reclamation.getIdCitizen());
            }

            ps.setString(7, reclamation.getPrivateMessage());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de la réclamation a échoué, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reclamation.setId(generatedKeys.getInt(1));
                    System.out.println("Réclamation ajoutée avec l'ID: " + reclamation.getId());
                } else {
                    throw new SQLException("La création de la réclamation a échoué, aucun ID obtenu.");
                }
            }
        }
    }

    @Override
    public void modifier(Reclamation reclamation) throws SQLException {
        System.out.println("Modification de la réclamation: " + reclamation);
        
        // Vérifier si la réclamation existe
        String checkQuery = "SELECT etat, date_traitement, id_reclamation FROM reclamations WHERE id_reclamation=?";
        try (PreparedStatement checkPs = con.prepareStatement(checkQuery)) {
            checkPs.setInt(1, reclamation.getId());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Réclamation non trouvée avec l'ID: " + reclamation.getId());
                }
                
                // Vérifier l'état actuel
                String currentEtat = rs.getString("etat");
                if (currentEtat != null && currentEtat.equalsIgnoreCase("traitee")) {
                    throw new SQLException("Cette réclamation a déjà été traitée et ne peut plus être modifiée.");
                }
            }
        }

        // Vérifier si l'ID citoyen existe
        if (reclamation.getIdCitizen() != 0) {
            if (!verifyUserExists(reclamation.getIdCitizen())) {
                throw new SQLException("L'ID citoyen " + reclamation.getIdCitizen() + " n'existe pas dans la base de données.");
            }
        }

        // Préparer la requête
        String req;
        if (reclamation.getEtat() != null && reclamation.getEtat().equalsIgnoreCase("traitee")) {
            req = "UPDATE reclamations SET titre=?, contenu=?, date_creation=?, etat=?, id_user=?, id_citizen=?, private_message=?, date_traitement=CURRENT_DATE WHERE id_reclamation=?";
        } else {
            req = "UPDATE reclamations SET titre=?, contenu=?, date_creation=?, etat=?, id_user=?, id_citizen=?, private_message=? WHERE id_reclamation=?";
        }

        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setString(1, reclamation.getTitre());
            ps.setString(2, reclamation.getContenu());
            ps.setDate(3, java.sql.Date.valueOf(reclamation.getDateCreation()));
            ps.setString(4, reclamation.getEtat());
            ps.setInt(5, reclamation.getIdUser());
            ps.setInt(6, reclamation.getIdCitizen());
            ps.setString(7, reclamation.getPrivateMessage());
            ps.setInt(8, reclamation.getId());

            try {
                int affectedRows = ps.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Aucune ligne n'a été modifiée pour l'ID: " + reclamation.getId());
                }
                System.out.println("Nombre de lignes modifiées: " + affectedRows);
            } catch (SQLException e) {
                System.err.println("Erreur lors de l'exécution de la requête: " + e.getMessage());
                System.err.println("Requête SQL: " + req);
                System.err.println("Paramètres:");
                System.err.println("Titre: " + reclamation.getTitre());
                System.err.println("Contenu: " + reclamation.getContenu());
                System.err.println("Date: " + reclamation.getDateCreation());
                System.err.println("État: " + reclamation.getEtat());
                System.err.println("ID User: " + reclamation.getIdUser());
                System.err.println("ID Citoyen: " + reclamation.getIdCitizen());
                System.err.println("Message privé: " + reclamation.getPrivateMessage());
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la préparation de la requête: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void supprimer(Reclamation reclamation) throws SQLException {
        System.out.println("Suppression de la réclamation avec l'ID: " + reclamation.getId());
        String req = "DELETE FROM reclamations WHERE id_reclamation=?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, reclamation.getId());
            
            int affectedRows = ps.executeUpdate();
            System.out.println("Nombre de lignes supprimées: " + affectedRows);
        }
    }

    @Override
    public List<Reclamation> recuperer() throws SQLException {
        System.out.println("Récupération de toutes les réclamations");
        List<Reclamation> reclamations = new ArrayList<>();
        String req = "SELECT * FROM reclamations ORDER BY date_creation DESC";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                LocalDate dateTraitement = null;
                try {
                    if (rs.getDate("date_traitement") != null) {
                        dateTraitement = rs.getDate("date_traitement").toLocalDate();
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur lors de la récupération de date_traitement: " + e.getMessage());
                }
                
                Reclamation r = new Reclamation(
                    rs.getInt("id_reclamation"),
                    rs.getString("titre"),
                    rs.getString("contenu"),
                    rs.getDate("date_creation").toLocalDate(),
                    dateTraitement,
                    rs.getInt("id_user"),
                    rs.getString("etat"),
                    rs.getInt("id_citizen"),
                    rs.getString("private_message")
                );
                reclamations.add(r);
                System.out.println("Réclamation chargée: " + r);
            }
        }
        System.out.println("Nombre total de réclamations récupérées: " + reclamations.size());
        return reclamations;
    }

    public List<Reclamation> searchByUserName(String userName) throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();
        String req = """
            SELECT r.* FROM reclamations r
            JOIN users u ON r.id_user = u.id_user
            WHERE LOWER(u.nom) LIKE LOWER(?) OR LOWER(u.prenom) LIKE LOWER(?)
            ORDER BY r.date_creation DESC
        """;
        
        try (PreparedStatement ps = con.prepareStatement(req)) {
            String searchPattern = "%" + userName + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate dateTraitement = null;
                    if (rs.getDate("date_traitement") != null) {
                        dateTraitement = rs.getDate("date_traitement").toLocalDate();
                    }
                    
                    Reclamation r = new Reclamation(
                        rs.getInt("id_reclamation"),
                        rs.getString("titre"),
                        rs.getString("contenu"),
                        rs.getDate("date_creation").toLocalDate(),
                        dateTraitement,
                        rs.getInt("id_user"),
                        rs.getString("etat"),
                        rs.getInt("id_citizen"),
                        rs.getString("private_message")
                    );
                    reclamations.add(r);
                }
            }
        }
        return reclamations;
    }

    public void updateReclamationState(int reclamationId, String newState) throws SQLException {
        String req = "UPDATE reclamations SET etat = ?, date_traitement = CURRENT_DATE WHERE id_reclamation = ? AND etat != 'traitee'";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setString(1, newState);
            ps.setInt(2, reclamationId);
            
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La réclamation est déjà traitée ou n'existe pas");
            }
        }
    }

    public void addPrivateMessage(int reclamationId, String message) throws SQLException {
        String req = "UPDATE reclamations SET private_message = ? WHERE id_reclamation = ?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setString(1, message);
            ps.setInt(2, reclamationId);
            ps.executeUpdate();
        }
    }
}
