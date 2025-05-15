package services;

import entities.User;
import utils.MyConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public class ServiceUser implements IService<User> {
    private Connection con;
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";

    public ServiceUser() {
        con = MyConnection.getInstance().getCnx();
        verifyTableExists();
    }

    private void verifyTableExists() {
        try {
            DatabaseMetaData metaData = con.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "users", null);
            
            if (!tables.next()) {
                System.out.println("Création de la table users...");
                createUsersTable();
                createDefaultUser();
            } else {
                System.out.println("La table users existe déjà");
                ensureDefaultUserExists();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createUsersTable() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                id_user INT AUTO_INCREMENT PRIMARY KEY,
                nom VARCHAR(50) NOT NULL,
                prenom VARCHAR(50) NOT NULL,
                email VARCHAR(100) NOT NULL UNIQUE,
                password VARCHAR(100) NOT NULL,
                role VARCHAR(20) DEFAULT 'USER',
                date_naissance DATE,
                phone_number VARCHAR(20)
            )
        """;
        
        try (Statement stmt = con.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Table users créée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDefaultUser() {
        String insertSQL = "INSERT INTO users (id_user, nom, prenom, email, password, role) VALUES (1, 'Admin', 'Admin', 'admin@example.com', 'admin123', 'ADMIN')";
        try (Statement stmt = con.createStatement()) {
            stmt.execute(insertSQL);
            System.out.println("Utilisateur par défaut créé avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de l'utilisateur par défaut: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureDefaultUserExists() {
        String checkSQL = "SELECT COUNT(*) FROM users WHERE id_user = 1";
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(checkSQL)) {
            if (rs.next() && rs.getInt(1) == 0) {
                createDefaultUser();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de l'utilisateur par défaut: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void ajouter(User user) throws SQLException {
        String req = "INSERT INTO users(nom, prenom, email, password, role) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(req);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        ps.setString(4, hashedPassword);
        ps.setString(5, user.getRole());
        ps.executeUpdate();
        System.out.println("Utilisateur ajouté");
    }

    @Override
    public void modifier(User user) throws SQLException {
        // If password is being updated, hash it
        String passwordToUse = user.getPassword();
        if (passwordToUse != null && !passwordToUse.isEmpty() && !passwordToUse.startsWith("$2a$")) {
            passwordToUse = BCrypt.hashpw(passwordToUse, BCrypt.gensalt());
        }

        String req = "UPDATE users SET nom=?, prenom=?, email=?, password=?, role=? WHERE id_user=?";
        PreparedStatement ps = con.prepareStatement(req);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, passwordToUse);
        ps.setString(5, user.getRole());
        ps.setInt(6, user.getId());
        ps.executeUpdate();
        System.out.println("Utilisateur modifié");
    }

    @Override
    public void supprimer(User user) throws SQLException {
        String req = "DELETE FROM users WHERE id_user=?";
        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, user.getId());
        ps.executeUpdate();
        System.out.println("Utilisateur supprimé");
    }

    @Override
    public List<User> recuperer() throws SQLException {
        List<User> users = new ArrayList<>();
        String req = "SELECT * FROM users";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            User u = new User(
                    rs.getInt("id_user"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getDate("date_de_naissance") != null ? rs.getDate("date_de_naissance").toLocalDate() : null,
                    rs.getString("phone_number")
            );
            users.add(u);
        }
        return users;
    }

    public boolean emailExists(String email) throws SQLException {
        String req = "SELECT COUNT(*) FROM users WHERE email = ?";
        PreparedStatement ps = con.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    public boolean updatePassword(String email, String newPassword) throws SQLException {
        String req = "UPDATE users SET password = ? WHERE email = ?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            ps.setString(1, hashedPassword);
            ps.setString(2, email);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public User getUserById(int userId) throws SQLException {
        String req = "SELECT * FROM users WHERE id_user = ?";
        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        
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
        return null;
    }

    public void validateUser(User user) throws Exception {
        // Validate age
        if (user.getDateNaissance() != null) {
            Period age = Period.between(user.getDateNaissance(), LocalDate.now());
            if (age.getYears() < 18) {
                throw new Exception("You must be 18 years old or older to access this");
            }
        }

        // Validate email
        if (!Pattern.matches(EMAIL_PATTERN, user.getEmail())) {
            throw new Exception("Invalid email format");
        }

        // Validate password
        if (!Pattern.matches(PASSWORD_PATTERN, user.getPassword())) {
            throw new Exception("Password must contain at least 8 characters, including uppercase, lowercase, numbers and special characters");
        }
    }

    public User authenticateUser(String email, String password) throws SQLException {
        String req = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setString(1, email);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    boolean isAuthenticated = false;
                    
                    // Check if stored password is a BCrypt hash
                    if (storedPassword.startsWith("$2a$")) {
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
                        return new User(
                            rs.getInt("id_user"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getDate("date_de_naissance") != null ? rs.getDate("date_de_naissance").toLocalDate() : null,
                            rs.getString("phone_number")
                        );
                    }
                }
            }
        }
        throw new SQLException("Invalid email or password");
    }

    public void updateUser(User user) throws SQLException {
        String req = "UPDATE users SET nom=?, prenom=?, email=?, password=?, date_de_naissance=?, phone_number=? WHERE id_user=?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setDate(5, java.sql.Date.valueOf(user.getDateNaissance()));
            ps.setString(6, user.getPhoneNumber());
            ps.setInt(7, user.getId());
            
            ps.executeUpdate();
        }
    }

    public List<User> getAdmins() throws SQLException {
        List<User> admins = new ArrayList<>();
        String req = "SELECT id_user, nom, prenom, email, role, date_de_naissance, phone_number FROM users WHERE role = 'admin'";
        try (PreparedStatement ps = con.prepareStatement(req);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getDate("date_de_naissance") != null ? rs.getDate("date_de_naissance").toLocalDate() : null,
                        rs.getString("phone_number")
                );
                admins.add(user);
            }
        }
        return admins;
    }
}