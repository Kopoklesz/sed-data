package com.employeemanager.dialog;

import com.employeemanager.database.config.ConnectionConfig;
import com.employeemanager.database.config.DatabaseType;
import com.employeemanager.service.impl.DatabaseConnectionService;
import com.employeemanager.util.AlertHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Adatbázis kapcsolat beállító dialógus
 */
public class DatabaseConnectionDialog extends Dialog<Void> {
    
    private final DatabaseConnectionService connectionService;
    
    // UI komponensek
    private final ListView<ConnectionConfig> connectionList = new ListView<>();
    private final ComboBox<DatabaseType> typeComboBox = new ComboBox<>();
    private final TextField nameField = new TextField();
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField();
    private final TextField databaseField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    
    // Firebase specifikus mezők
    private final TextField projectIdField = new TextField();
    private final TextField databaseUrlField = new TextField();
    private final TextField serviceAccountField = new TextField();
    private final Button browseButton = new Button("Tallózás...");
    
    // Gombok
    private final Button addButton = new Button("Hozzáadás");
    private final Button updateButton = new Button("Frissítés");
    private final Button deleteButton = new Button("Törlés");
    private final Button testButton = new Button("Kapcsolat tesztelése");
    private final Button activateButton = new Button("Aktiválás");
    
    // Form konténerek
    private final GridPane sqlFormGrid = new GridPane();
    private final GridPane firebaseFormGrid = new GridPane();
    private final StackPane formContainer = new StackPane();
    
    private ConnectionConfig editingConnection = null;
    
    public DatabaseConnectionDialog(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
        
        setTitle("Adatbázis kapcsolat beállító");
        setHeaderText("Adatbázis kapcsolatok kezelése");
        
        setupDialog();
        loadConnections();
    }
    
    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);
        dialogPane.setPrefWidth(900);
        dialogPane.setPrefHeight(600);
        
        // Fő konténer
        HBox mainContainer = new HBox(15);
        mainContainer.setPadding(new Insets(10));
        
        // Bal oldal - Kapcsolatok listája
        VBox leftPanel = createLeftPanel();
        
        // Jobb oldal - Kapcsolat űrlap
        VBox rightPanel = createRightPanel();
        
        mainContainer.getChildren().addAll(leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.NEVER);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        
        dialogPane.setContent(mainContainer);
        
        // Eseménykezelők
        setupEventHandlers();
    }
    
    private VBox createLeftPanel() {
        VBox panel = new VBox(10);
        panel.setPrefWidth(250);
        panel.setMinWidth(250);
        
        Label listLabel = new Label("Mentett kapcsolatok:");
        listLabel.setStyle("-fx-font-weight: bold;");
        
        connectionList.setPrefHeight(400);
        connectionList.setCellFactory(lv -> new ConnectionListCell());
        
        panel.getChildren().addAll(listLabel, connectionList);
        return panel;
    }
    
    private VBox createRightPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(0, 0, 0, 10));
        
        // Kapcsolat típus választó
        HBox typeBox = new HBox(10);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label("Típus:");
        typeLabel.setPrefWidth(120);
        typeComboBox.setPrefWidth(200);
        typeComboBox.setItems(FXCollections.observableArrayList(DatabaseType.values()));
        typeBox.getChildren().addAll(typeLabel, typeComboBox);
        
        // Kapcsolat neve
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Kapcsolat neve:");
        nameLabel.setPrefWidth(120);
        nameField.setPrefWidth(300);
        nameField.setPromptText("pl. Teszt MySQL szerver");
        nameBox.getChildren().addAll(nameLabel, nameField);
        
        // SQL form
        setupSqlForm();
        
        // Firebase form
        setupFirebaseForm();
        
        // Form konténer (SQL és Firebase form-ok itt váltakoznak)
        formContainer.getChildren().addAll(sqlFormGrid, firebaseFormGrid);
        
        // Gombok
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(
            testButton, addButton, updateButton, deleteButton, activateButton
        );
        
        // Progress indicator a teszteléshez
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(30, 30);
        
        HBox testBox = new HBox(10);
        testBox.setAlignment(Pos.CENTER_LEFT);
        testBox.getChildren().addAll(progressIndicator);
        
        panel.getChildren().addAll(
            typeBox, nameBox, formContainer, buttonBox, testBox
        );
        
        return panel;
    }
    
    private void setupSqlForm() {
        sqlFormGrid.setHgap(10);
        sqlFormGrid.setVgap(10);
        sqlFormGrid.setPadding(new Insets(10, 0, 0, 0));
        
        int row = 0;
        
        // Host
        Label hostLabel = new Label("Szerver:");
        hostField.setPromptText("localhost");
        hostField.setPrefWidth(300);
        sqlFormGrid.add(hostLabel, 0, row);
        sqlFormGrid.add(hostField, 1, row++);
        
        // Port
        Label portLabel = new Label("Port:");
        portField.setPromptText("3306 (MySQL) / 5432 (PostgreSQL)");
        portField.setPrefWidth(300);
        sqlFormGrid.add(portLabel, 0, row);
        sqlFormGrid.add(portField, 1, row++);
        
        // Database
        Label dbLabel = new Label("Adatbázis:");
        databaseField.setPromptText("employeedb");
        databaseField.setPrefWidth(300);
        sqlFormGrid.add(dbLabel, 0, row);
        sqlFormGrid.add(databaseField, 1, row++);
        
        // Username
        Label userLabel = new Label("Felhasználó:");
        usernameField.setPromptText("root");
        usernameField.setPrefWidth(300);
        sqlFormGrid.add(userLabel, 0, row);
        sqlFormGrid.add(usernameField, 1, row++);
        
        // Password
        Label passLabel = new Label("Jelszó:");
        passwordField.setPrefWidth(300);
        sqlFormGrid.add(passLabel, 0, row);
        sqlFormGrid.add(passwordField, 1, row++);
        
        // Oszlop szélességek
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(300);
        sqlFormGrid.getColumnConstraints().addAll(col1, col2);
    }
    
    private void setupFirebaseForm() {
        firebaseFormGrid.setHgap(10);
        firebaseFormGrid.setVgap(10);
        firebaseFormGrid.setPadding(new Insets(10, 0, 0, 0));
        firebaseFormGrid.setVisible(false);
        
        int row = 0;
        
        // Project ID
        Label projectLabel = new Label("Project ID:");
        projectIdField.setPromptText("employee-manager-e70b6");
        projectIdField.setPrefWidth(300);
        firebaseFormGrid.add(projectLabel, 0, row);
        firebaseFormGrid.add(projectIdField, 1, row++);
        
        // Database URL
        Label urlLabel = new Label("Database URL:");
        databaseUrlField.setPromptText("https://project-id.firebaseio.com");
        databaseUrlField.setPrefWidth(300);
        firebaseFormGrid.add(urlLabel, 0, row);
        firebaseFormGrid.add(databaseUrlField, 1, row++);
        
        // Service Account
        Label saLabel = new Label("Service Account:");
        serviceAccountField.setPromptText("service-account.json");
        serviceAccountField.setPrefWidth(240);
        HBox saBox = new HBox(5);
        saBox.getChildren().addAll(serviceAccountField, browseButton);
        firebaseFormGrid.add(saLabel, 0, row);
        firebaseFormGrid.add(saBox, 1, row++);
        
        // Oszlop szélességek
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(300);
        firebaseFormGrid.getColumnConstraints().addAll(col1, col2);
    }
    
    private void setupEventHandlers() {
        // Típus választó
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                switchForm(newVal);
                // Port automatikus kitöltése
                if (portField.getText().isEmpty()) {
                    Integer defaultPort = ConnectionConfig.getDefaultPort(newVal);
                    if (defaultPort != null) {
                        portField.setText(defaultPort.toString());
                    }
                }
            }
        });
        
        // Kapcsolat lista választás
        connectionList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadConnectionToForm(newVal);
                }
            }
        );
        
        // Gombok
        addButton.setOnAction(e -> addConnection());
        updateButton.setOnAction(e -> updateConnection());
        deleteButton.setOnAction(e -> deleteConnection());
        testButton.setOnAction(e -> testConnection());
        activateButton.setOnAction(e -> activateConnection());
        
        // Browse gomb Firebase service account fájlhoz
        browseButton.setOnAction(e -> browseServiceAccount());
    }
    
    private void switchForm(DatabaseType type) {
        boolean isFirebase = type == DatabaseType.FIREBASE;
        sqlFormGrid.setVisible(!isFirebase);
        sqlFormGrid.setManaged(!isFirebase);
        firebaseFormGrid.setVisible(isFirebase);
        firebaseFormGrid.setManaged(isFirebase);
    }
    
    private void loadConnections() {
        connectionList.getItems().clear();
        connectionList.getItems().addAll(connectionService.getAllConnections());
        
        // Aktív kapcsolat kijelölése
        Optional<ConnectionConfig> activeConnection = connectionService.getActiveConnection();
        activeConnection.ifPresent(config -> 
            connectionList.getSelectionModel().select(config));
    }
    
    private void loadConnectionToForm(ConnectionConfig config) {
        editingConnection = config;
        
        nameField.setText(config.getName());
        typeComboBox.setValue(config.getType());
        
        if (config.getType() == DatabaseType.FIREBASE) {
            projectIdField.setText(config.getFirebaseProjectId());
            databaseUrlField.setText(config.getFirebaseDatabaseUrl());
            serviceAccountField.setText(config.getFirebaseServiceAccountPath());
        } else {
            hostField.setText(config.getHost());
            if (config.getPort() != null) {
                portField.setText(config.getPort().toString());
            }
            databaseField.setText(config.getDatabase());
            usernameField.setText(config.getUsername());
            passwordField.setText(config.getPassword());
        }
        
        // Gombok állapota
        updateButton.setDisable(false);
        deleteButton.setDisable(config.isActive());
        activateButton.setDisable(config.isActive());
    }
    
    private ConnectionConfig buildConfigFromForm() {
        ConnectionConfig.ConnectionConfigBuilder builder = ConnectionConfig.builder()
            .name(nameField.getText().trim())
            .type(typeComboBox.getValue());
        
        if (typeComboBox.getValue() == DatabaseType.FIREBASE) {
            builder.firebaseProjectId(projectIdField.getText().trim())
                   .firebaseDatabaseUrl(databaseUrlField.getText().trim())
                   .firebaseServiceAccountPath(serviceAccountField.getText().trim());
        } else {
            builder.host(hostField.getText().trim())
                   .port(portField.getText().isEmpty() ? null : Integer.parseInt(portField.getText()))
                   .database(databaseField.getText().trim())
                   .username(usernameField.getText().trim())
                   .password(passwordField.getText());
        }
        
        return builder.build();
    }
    
    private void addConnection() {
        try {
            ConnectionConfig config = buildConfigFromForm();
            
            if (!config.isValid()) {
                AlertHelper.showWarning("Érvénytelen konfiguráció", 
                    "Kérem töltse ki az összes kötelező mezőt!");
                return;
            }
            
            connectionService.addConnection(config);
            loadConnections();
            clearForm();
            
            AlertHelper.showInformation("Sikeres hozzáadás", 
                "Kapcsolat hozzáadva", 
                "Az új kapcsolat sikeresen létrehozva.");
            
        } catch (Exception e) {
            AlertHelper.showError("Hiba", 
                "Nem sikerült hozzáadni a kapcsolatot", 
                e.getMessage());
        }
    }
    
    private void updateConnection() {
        if (editingConnection == null) {
            return;
        }
        
        try {
            ConnectionConfig config = buildConfigFromForm();
            
            if (!config.isValid()) {
                AlertHelper.showWarning("Érvénytelen konfiguráció", 
                    "Kérem töltse ki az összes kötelező mezőt!");
                return;
            }
            
            connectionService.updateConnection(editingConnection.getName(), config);
            loadConnections();
            
            AlertHelper.showInformation("Sikeres frissítés", 
                "Kapcsolat frissítve", 
                "A kapcsolat sikeresen frissítve.");
            
        } catch (Exception e) {
            AlertHelper.showError("Hiba", 
                "Nem sikerült frissíteni a kapcsolatot", 
                e.getMessage());
        }
    }
    
    private void deleteConnection() {
        if (editingConnection == null) {
            return;
        }
        
        if (editingConnection.isActive()) {
            AlertHelper.showWarning("Nem törölhető", 
                "Az aktív kapcsolat nem törölhető. Aktiváljon egy másik kapcsolatot először!");
            return;
        }
        
        boolean confirm = AlertHelper.showConfirmation(
            "Törlés megerősítése",
            "Biztosan törli a kapcsolatot?",
            "Kapcsolat: " + editingConnection.getName()
        );
        
        if (confirm) {
            try {
                connectionService.removeConnection(editingConnection.getName());
                loadConnections();
                clearForm();
                
                AlertHelper.showInformation("Sikeres törlés", 
                    "Kapcsolat törölve", 
                    "A kapcsolat sikeresen törölve.");
                
            } catch (Exception e) {
                AlertHelper.showError("Hiba", 
                    "Nem sikerült törölni a kapcsolatot", 
                    e.getMessage());
            }
        }
    }
    
    private void testConnection() {
        try {
            ConnectionConfig config = buildConfigFromForm();
            
            if (!config.isValid()) {
                AlertHelper.showWarning("Érvénytelen konfiguráció", 
                    "Kérem töltse ki az összes kötelező mezőt!");
                return;
            }
            
            // Progress indicator megjelenítése
            testButton.setDisable(true);
            testButton.setText("Tesztelés...");
            
            // Aszinkron tesztelés
            CompletableFuture.supplyAsync(() -> 
                connectionService.testConnection(config)
            ).thenAcceptAsync(success -> {
                Platform.runLater(() -> {
                    testButton.setDisable(false);
                    testButton.setText("Kapcsolat tesztelése");
                    
                    if (success) {
                        AlertHelper.showInformation("Sikeres kapcsolat", 
                            "A kapcsolat tesztelése sikeres!", 
                            "Az adatbázis elérhető és a kapcsolat létrehozható.");
                    } else {
                        AlertHelper.showError("Kapcsolódási hiba", 
                            "Nem sikerült kapcsolódni az adatbázishoz", 
                            "Ellenőrizze a kapcsolat beállításait!");
                    }
                });
            }).exceptionally(e -> {
                Platform.runLater(() -> {
                    testButton.setDisable(false);
                    testButton.setText("Kapcsolat tesztelése");
                    
                    AlertHelper.showError("Hiba", 
                        "Hiba történt a tesztelés során", 
                        e.getMessage());
                });
                return null;
            });
            
        } catch (Exception e) {
            AlertHelper.showError("Hiba", 
                "Nem sikerült tesztelni a kapcsolatot", 
                e.getMessage());
        }
    }
    
    private void activateConnection() {
        if (editingConnection == null) {
            return;
        }
        
        if (editingConnection.isActive()) {
            AlertHelper.showInformation("Már aktív", 
                "Ez a kapcsolat már aktív", 
                "Válasszon egy másik kapcsolatot az aktiváláshoz.");
            return;
        }
        
        activateButton.setDisable(true);
        activateButton.setText("Aktiválás...");
        
        // Aszinkron aktiválás
        CompletableFuture.supplyAsync(() -> 
            connectionService.activateConnection(editingConnection.getName())
        ).thenAcceptAsync(success -> {
            Platform.runLater(() -> {
                activateButton.setDisable(false);
                activateButton.setText("Aktiválás");
                
                if (success) {
                    loadConnections();
                    
                    AlertHelper.showInformation("Sikeres aktiválás", 
                        "Kapcsolat aktiválva", 
                        "Az új adatbázis kapcsolat aktív. Az alkalmazás mostantól ezt használja.");
                } else {
                    AlertHelper.showError("Aktiválási hiba", 
                        "Nem sikerült aktiválni a kapcsolatot", 
                        "Ellenőrizze, hogy a kapcsolat elérhető-e!");
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                activateButton.setDisable(false);
                activateButton.setText("Aktiválás");
                
                AlertHelper.showError("Hiba", 
                    "Hiba történt az aktiválás során", 
                    e.getMessage());
            });
            return null;
        });
    }
    
    private void browseServiceAccount() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Service Account JSON fájl kiválasztása");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON fájlok", "*.json")
        );
        
        File file = fileChooser.showOpenDialog(getOwner());
        if (file != null) {
            serviceAccountField.setText(file.getAbsolutePath());
        }
    }
    
    private void clearForm() {
        editingConnection = null;
        nameField.clear();
        typeComboBox.getSelectionModel().clearSelection();
        hostField.clear();
        portField.clear();
        databaseField.clear();
        usernameField.clear();
        passwordField.clear();
        projectIdField.clear();
        databaseUrlField.clear();
        serviceAccountField.clear();
        
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        activateButton.setDisable(true);
    }
    
    /**
     * Custom ListCell a kapcsolatok megjelenítéséhez
     */
    private class ConnectionListCell extends ListCell<ConnectionConfig> {
        @Override
        protected void updateItem(ConnectionConfig item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                VBox content = new VBox(2);
                
                Label nameLabel = new Label(item.getName());
                nameLabel.setStyle("-fx-font-weight: bold;");
                
                Label typeLabel = new Label(item.getType().getDisplayName());
                typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
                
                if (item.isActive()) {
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
                    typeLabel.setText(typeLabel.getText() + " (AKTÍV)");
                }
                
                content.getChildren().addAll(nameLabel, typeLabel);
                setGraphic(content);
            }
        }
    }
}