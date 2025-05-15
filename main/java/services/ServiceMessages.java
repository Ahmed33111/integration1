package services;

import entities.Message;
import entities.User;
import utils.MyConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceMessages {
    private Connection con;

    public ServiceMessages() {
        con = MyConnection.getInstance().getCnx();
        verifyTableExists();
    }

    private void verifyTableExists() {
        try {
            DatabaseMetaData metaData = con.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "messages", null);
            
            if (!tables.next()) {
                System.out.println("Création de la table messages...");
                createMessagesTable();
            } else {
                System.out.println("La table messages existe déjà");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createMessagesTable() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS messages (
                id_message INT AUTO_INCREMENT PRIMARY KEY,
                id_sender INT NOT NULL,
                id_receiver INT NOT NULL,
                content TEXT NOT NULL,
                timestamp DATETIME NOT NULL,
                is_read BOOLEAN DEFAULT FALSE,
                FOREIGN KEY (id_sender) REFERENCES users(id_user) ON DELETE CASCADE,
                FOREIGN KEY (id_receiver) REFERENCES users(id_user) ON DELETE CASCADE
            )
        """;
        
        try (Statement stmt = con.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Table messages créée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) throws SQLException {
        String req = "INSERT INTO messages(id_sender, id_receiver, content, timestamp, is_read) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, message.getIdSender());
            ps.setInt(2, message.getIdReceiver());
            ps.setString(3, message.getContent());
            ps.setTimestamp(4, Timestamp.valueOf(message.getTimestamp()));
            ps.setBoolean(5, message.isRead());
            
            ps.executeUpdate();
            
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Message> getConversation(int userId1, int userId2) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String req = """
            SELECT m.*, CONCAT(u.nom, ' ', u.prenom) as sender_name 
            FROM messages m
            JOIN users u ON m.id_sender = u.id_user
            WHERE (m.id_sender = ? AND m.id_receiver = ?)
               OR (m.id_sender = ? AND m.id_receiver = ?)
            ORDER BY m.timestamp ASC
        """;
        
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message(
                        rs.getInt("id_message"),
                        rs.getInt("id_sender"),
                        rs.getInt("id_receiver"),
                        rs.getString("content"),
                        rs.getTimestamp("timestamp").toLocalDateTime(),
                        rs.getBoolean("is_read")
                    );
                    message.setSenderName(rs.getString("sender_name"));
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    public void markAsRead(int messageId) throws SQLException {
        String req = "UPDATE messages SET is_read = TRUE WHERE id_message = ?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        }
    }

    public int getUnreadMessageCount(int userId) throws SQLException {
        String req = "SELECT COUNT(*) FROM messages WHERE id_receiver = ? AND is_read = FALSE";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<User> getMessagedAdmins(int userId) throws SQLException {
        List<User> admins = new ArrayList<>();
        String req = """
            SELECT DISTINCT u.id_user, u.nom, u.prenom, u.email, u.role
            FROM users u
            JOIN messages m ON (m.id_sender = u.id_user AND m.id_receiver = ?)
                            OR (m.id_receiver = u.id_user AND m.id_sender = ?)
            WHERE u.role = 'admin'
        """;

        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User(
                            rs.getInt("id_user"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("role"),
                            null, // dateNaissance
                            null  // phoneNumber
                    );
                    admins.add(user);
                }
            }
        }
        return admins;
    }

}
