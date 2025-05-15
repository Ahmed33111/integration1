package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private static MyConnection instance;
    private Connection cnx;

    private final String URL = "jdbc:mysql://localhost:3306/module";
    private final String USER = "root";
    private final String PASSWORD = "";

    private MyConnection() {
        try {
            // Charger explicitement le driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver MySQL chargé avec succès");

            // Établir la connexion
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            if (cnx != null) {
                System.out.println("Connexion à la base de données établie avec succès");
                System.out.println("URL: " + URL);
            } else {
                System.err.println("Échec de la connexion à la base de données!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL non trouvé: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Erreur de connexion à la base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                System.out.println("Reconnexion à la base de données...");
                cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la reconnexion: " + e.getMessage());
            e.printStackTrace();
        }
        return cnx;
    }

    public void close() {
        try {
            if (cnx != null && !cnx.isClosed()) {
                cnx.close();
                System.out.println("Connexion à la base de données fermée");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}