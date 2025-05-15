package entities;

import java.time.LocalDate;

public class Reclamation {
    private int id;
    private String titre, contenu;
    private LocalDate dateCreation;
    private LocalDate dateTraitement;
    private int idUser;
    private String etat;
    private int idCitizen;
    private String privateMessage;

    // Constructeur pour créer une nouvelle réclamation (sans ID)
    public Reclamation(String titre, String contenu, LocalDate dateCreation, int idUser, String etat, int idCitizen, String privateMessage) {
        this.titre = titre;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.dateTraitement = null;
        this.idUser = idUser;
        this.etat = etat;
        this.idCitizen = idCitizen;
        this.privateMessage = privateMessage;
    }

    // Constructeur pour charger une réclamation existante (avec ID)
    public Reclamation(int id, String titre, String contenu, LocalDate dateCreation, LocalDate dateTraitement, int idUser, String etat, int idCitizen, String privateMessage) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.dateTraitement = dateTraitement;
        this.idUser = idUser;
        this.etat = etat;
        this.idCitizen = idCitizen;
        this.privateMessage = privateMessage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public LocalDate getDateTraitement() {
        return dateTraitement;
    }

    public void setDateTraitement(LocalDate dateTraitement) {
        this.dateTraitement = dateTraitement;
    }

    public String getEtat() {
        return etat;
    }

    public int getIdCitizen() {
        return idCitizen;
    }

    public void setIdCitizen(int idCitizen) {
        this.idCitizen = idCitizen;
    }

    public String getPrivateMessage() {
        return privateMessage;
    }

    public void setPrivateMessage(String privateMessage) {
        this.privateMessage = privateMessage;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    @Override
    public String toString() {
        return "Reclamation{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", contenu='" + contenu + '\'' +
                ", dateCreation=" + dateCreation +
                ", dateTraitement=" + dateTraitement +
                ", idUser=" + idUser +
                ", etat='" + etat + '\'' +
                ", idCitizen=" + idCitizen +
                ", privateMessage='" + privateMessage + '\'' +
                '}';
    }
}
