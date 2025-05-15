package entities;

import java.time.LocalDate;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String role;
    private LocalDate dateNaissance;
    private String phoneNumber;
    
    public User() {
    }
    
    public User(int id, String nom, String prenom, String email, String password, String role) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
    }
    
    public User(String nom, String prenom, String email, String password, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
    }
    
    public User(int id, String nom, String prenom, String email, String password, LocalDate dateNaissance) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.dateNaissance = dateNaissance;
    }
    
    public User(String nom, String prenom, String email, String password, LocalDate dateNaissance) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.dateNaissance = dateNaissance;
    }
    
    public User(int id, String nom, String prenom, String email, String password, LocalDate dateNaissance, String phoneNumber) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.dateNaissance = dateNaissance;
        this.phoneNumber = phoneNumber;
    }
    
    public User(String nom, String prenom, String email, String password, LocalDate dateNaissance, String phoneNumber) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.dateNaissance = dateNaissance;
        this.phoneNumber = phoneNumber;
    }
    
    public User(int id, String nom, String prenom, String email, String password, String role, LocalDate dateNaissance, String phoneNumber) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.dateNaissance = dateNaissance;
        this.phoneNumber = phoneNumber;
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public String getNom() {
        return nom;
    }
    
    public String getPrenom() {
        return prenom;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getRole() {
        return role;
    }
    
    public LocalDate getDateNaissance() {
        return dateNaissance;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    // Setters
    public void setId(int id) {
        this.id = id;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", role='" + role + '\'' +
                ", dateNaissance=" + dateNaissance +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
