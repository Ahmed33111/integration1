package services;

import entities.User;
import utils.MyConnection;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private Connection conn;
    
    public UserService() {
        conn = MyConnection.getInstance().getCnx();
        checkTableStructure();
        ensureDefaultAdminExists();
    }
    
    private void checkTableStructure() {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "users", new String[] {"TABLE"});
            
            if (!tables.next()) {
                System.out.println("La table 'users' n'existe pas. Création de la table...");
                createUsersTable();
            } else {
                System.out.println("La table 'users' existe.");
                // Vérifier la structure des colonnes
                ResultSet columns = metaData.getColumns(null, null, "users", null);
                while (columns.next()) {
                    System.out.println("Colonne trouvée: " + columns.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la structure de la table : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createUsersTable() {
        String createTableSQL = "CREATE TABLE users (" +
                "id_user INT AUTO_INCREMENT PRIMARY KEY, " +
                "nom VARCHAR(50) NOT NULL, " +
                "prenom VARCHAR(50) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL, " +
                "date_de_naissance DATE NOT NULL, " +
                "role VARCHAR(20) NOT NULL" +
                ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Table 'users' créée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la table : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void ensureDefaultAdminExists() {
        String checkAdmin = "SELECT COUNT(*) FROM users WHERE nom='admin' AND prenom='admin' AND email='admin@gmail.com' AND role='admin'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(checkAdmin)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String hashedPassword = BCrypt.hashpw("admin", BCrypt.gensalt());
                String insertAdmin = "INSERT INTO users (nom, prenom, email, password, date_de_naissance, role) VALUES ('admin', 'admin', 'admin@gmail.com', '"+hashedPassword+"', '2000-01-01', 'admin')";
                st.executeUpdate(insertAdmin);
                System.out.println("Admin par défaut créé.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de l'admin par défaut : " + e.getMessage());
        }
    }
    
    public boolean createUser(User user) {
        System.out.println("Tentative de création d'un nouvel utilisateur : " + user);
        // Restriction : un seul admin avec nom/prénom/email/password fixes
        if (user.getNom().equalsIgnoreCase("admin") && user.getPrenom().equalsIgnoreCase("admin") && user.getEmail().equalsIgnoreCase("admin@gmail.com") && user.getPassword().equals("admin")) {
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE LOWER(nom) = 'admin' AND LOWER(prenom) = 'admin' AND LOWER(email) = 'admin@gmail.com'";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(checkAdmin)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.err.println("Il existe déjà un administrateur avec ces informations.");
                    return false;
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors de la vérification de l'admin : " + e.getMessage());
                return false;
            }
            // Création de l'admin
            String query = "INSERT INTO users (nom, prenom, email, password, date_de_naissance, role) VALUES (?, ?, ?, ?, ?, 'admin')";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, user.getNom());
                pst.setString(2, user.getPrenom());
                pst.setString(3, user.getEmail());
                String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
                pst.setString(4, hashedPassword);
                pst.setDate(5, java.sql.Date.valueOf(user.getDateNaissance()));
                int rowsAffected = pst.executeUpdate();
                System.out.println("Admin créé. Nombre de lignes affectées : " + rowsAffected);
                return rowsAffected > 0;
            } catch (SQLException e) {
                System.err.println("Erreur lors de la création de l'admin : " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // Tous les autres sont citoyens
            // Vérification de l'âge >= 18 ans
            if (user.getDateNaissance() == null) {
                System.err.println("Date de naissance manquante.");
                return false;
            }
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.Period age = java.time.Period.between(user.getDateNaissance(), now);
            if (age.getYears() < 18) {
                System.err.println("L'utilisateur doit avoir au moins 18 ans.");
                return false;
            }
            String query = "INSERT INTO users (nom, prenom, email, password, date_de_naissance, phone_number, role) VALUES (?, ?, ?, ?, ?, ?, 'citoyen')";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, user.getNom());
                pst.setString(2, user.getPrenom());
                pst.setString(3, user.getEmail());
                String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
                pst.setString(4, hashedPassword);
                pst.setDate(5, java.sql.Date.valueOf(user.getDateNaissance()));
                pst.setString(6, user.getPhoneNumber());
                int rowsAffected = pst.executeUpdate();
                System.out.println("Nombre de lignes affectées : " + rowsAffected);
                return rowsAffected > 0;
            } catch (SQLException e) {
                System.err.println("Erreur lors de la création du citoyen : " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
    
    public User authenticate(String email, String password) {
        System.out.println("Tentative d'authentification pour l'email : " + email);
        String query = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, email);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Utilisateur trouvé dans la base de données");
                    String storedPassword = rs.getString("password");
                    boolean isAuthenticated = false;
                    
                    // Check if stored password is a BCrypt hash (starts with $2a$)
                    if (storedPassword.startsWith("$2a$")) {
                        // Verify with BCrypt
                        try {
                            isAuthenticated = BCrypt.checkpw(password, storedPassword);
                        } catch (IllegalArgumentException e) {
                            System.out.println("Erreur lors de la vérification BCrypt: " + e.getMessage());
                            isAuthenticated = false;
                        }
                    } else {
                        // Plain text comparison as fallback
                        isAuthenticated = password.equals(storedPassword);
                        
                        // If authentication successful with plain text, upgrade to BCrypt
                        if (isAuthenticated) {
                            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                            updatePassword(email, hashedPassword);
                            System.out.println("Password upgraded to BCrypt hash");
                        }
                    }
                    
                    if (isAuthenticated) {
                        System.out.println("Mot de passe correct");
                        return new User(
                            rs.getInt("id_user"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("role")
                        );
                    } else {
                        System.out.println("Mot de passe incorrect");
                    }
                } else {
                    System.out.println("Aucun utilisateur trouvé avec cet email");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'authentification : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public User getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, email);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
        }
        return null;
    }
    
    public boolean updatePassword(String email, String newPassword) {
        String query = "UPDATE users SET password = ? WHERE email = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            // If the password is already hashed (starts with $2a$), use it directly
            String passwordToStore;
            if (newPassword.startsWith("$2a$")) {
                passwordToStore = newPassword;
            } else {
                // Otherwise, hash it
                passwordToStore = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            }
            
            pst.setString(1, passwordToStore);
            pst.setString(2, email);
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }
} 