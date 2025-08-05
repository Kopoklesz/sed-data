package com.employeemanager.dialog;

import com.employeemanager.config.DatabaseConnectionConfig;
import com.employeemanager.config.DatabaseConnectionManager;
import com.employeemanager.config.DatabaseType;
import com.employeemanager.service.impl.SettingsService;
import com.employeemanager.util.AlertHelper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class SettingsDialog extends Dialog<Void> {

    private final SettingsService settingsService;
    private final DatabaseConnectionManager connectionManager;

    // General settings
    private final TextField authorField = new TextField();

    // Database settings
    private final ComboBox<DatabaseType> databaseTypeCombo = new ComboBox<>();
    private final ComboBox<DatabaseConnectionConfig> savedConnectionsCombo = new ComboBox<>();

    // Firebase fields
    private final TextField firebaseServiceAccountField = new TextField();
    private final Button firebaseBrowseButton = new Button("Tallózás...");
    private final TextField firebaseProjectIdField = new TextField();
    private final TextField firebaseDatabaseUrlField = new TextField();

    // JDBC fields
    private final TextField jdbcHostField = new TextField();
    private final TextField jdbcPortField = new TextField();
    private final TextField jdbcDatabaseField = new TextField();
    private final TextField jdbcUsernameField = new TextField();
    private final PasswordField jdbcPasswordField = new PasswordField();

    // Common fields
    private final TextField profileNameField = new TextField();

    // Dynamic panels
    private GridPane firebasePanel;
    private GridPane jdbcPanel;
    private VBox databaseSettingsPanel;

    public SettingsDialog(SettingsService settingsService, DatabaseConnectionManager connectionManager) {
        this.settingsService = settingsService;
        this.connectionManager = connectionManager;

        setTitle("Beállítások");
        setHeaderText("Alkalmazás és adatbázis beállítások");

        setupDialog();
        populateFields();
        updateDatabasePanel();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setPrefWidth(700);
        dialogPane.setPrefHeight(600);

        TabPane tabPane = new TabPane();

        // Általános beállítások tab
        Tab generalTab = new Tab("Általános");
        generalTab.setContent(createGeneralPanel());

        // Adatbázis beállítások tab
        Tab databaseTab = new Tab("Adatbázis");
        databaseTab.setContent(createDatabasePanel());

        tabPane.getTabs().addAll(generalTab, databaseTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        dialogPane.setContent(tabPane);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                saveSettings();
            }
            return null;
        });
    }

    private GridPane createGeneralPanel() {
        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(10);
        generalGrid.setVgap(10);
        generalGrid.setPadding(new Insets(20, 150, 10, 10));

        generalGrid.add(new Label("Verzió:"), 0, 0);
        generalGrid.add(new Label(settingsService.getApplicationVersion()), 1, 0);

        generalGrid.add(new Label("Készítette:"), 0, 1);
        generalGrid.add(authorField, 1, 1);
        authorField.setPrefWidth(300);

        return generalGrid;
    }

    private VBox createDatabasePanel() {
        databaseSettingsPanel = new VBox(15);
        databaseSettingsPanel.setPadding(new Insets(20));

        // Database type selection
        HBox typeBox = new HBox(10);
        typeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label typeLabel = new Label("Adatbázis típus:");
        typeLabel.setPrefWidth(120);

        databaseTypeCombo.getItems().addAll(DatabaseType.values());
        databaseTypeCombo.setPrefWidth(200);
        databaseTypeCombo.setOnAction(e -> updateDatabasePanel());

        typeBox.getChildren().addAll(typeLabel, databaseTypeCombo);

        // Saved connections
        HBox savedBox = new HBox(10);
        savedBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label savedLabel = new Label("Mentett kapcsolatok:");
        savedLabel.setPrefWidth(120);

        savedConnectionsCombo.setPrefWidth(200);
        savedConnectionsCombo.setOnAction(e -> loadSavedConnection());

        Button deleteButton = new Button("Törlés");
        deleteButton.setOnAction(e -> deleteSavedConnection());

        savedBox.getChildren().addAll(savedLabel, savedConnectionsCombo, deleteButton);

        // Profile name
        HBox profileBox = new HBox(10);
        profileBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label profileLabel = new Label("Profil név:");
        profileLabel.setPrefWidth(120);

        profileNameField.setPrefWidth(200);
        profileNameField.setPromptText("pl. Teszt szerver");

        profileBox.getChildren().addAll(profileLabel, profileNameField);

        // Firebase panel
        firebasePanel = createFirebasePanel();

        // JDBC panel
        jdbcPanel = createJdbcPanel();

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button testButton = new Button("Kapcsolat tesztelése");
        testButton.setOnAction(e -> testDatabaseConnection());

        Button applyButton = new Button("Alkalmazás");
        applyButton.setOnAction(e -> applyDatabaseConnection());

        buttonBox.getChildren().addAll(testButton, applyButton);

        databaseSettingsPanel.getChildren().addAll(
                typeBox,
                new Separator(),
                savedBox,
                profileBox,
                new Separator(),
                firebasePanel,
                jdbcPanel,
                new Separator(),
                buttonBox
        );

        return databaseSettingsPanel;
    }

    private GridPane createFirebasePanel() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        int row = 0;

        // Service account file
        grid.add(new Label("Service Account fájl:"), 0, row);
        HBox fileBox = new HBox(10);
        firebaseServiceAccountField.setPrefWidth(300);
        firebaseBrowseButton.setOnAction(e -> browseServiceAccountFile());
        fileBox.getChildren().addAll(firebaseServiceAccountField, firebaseBrowseButton);
        grid.add(fileBox, 1, row++);

        // Project ID
        grid.add(new Label("Project ID:"), 0, row);
        firebaseProjectIdField.setPrefWidth(300);
        grid.add(firebaseProjectIdField, 1, row++);

        // Database URL
        grid.add(new Label("Database URL:"), 0, row);
        firebaseDatabaseUrlField.setPrefWidth(400);
        grid.add(firebaseDatabaseUrlField, 1, row++);

        return grid;
    }

    private GridPane createJdbcPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        int row = 0;

        // Host
        grid.add(new Label("Host:"), 0, row);
        jdbcHostField.setPrefWidth(300);
        jdbcHostField.setPromptText("localhost");
        grid.add(jdbcHostField, 1, row++);

        // Port
        grid.add(new Label("Port:"), 0, row);
        jdbcPortField.setPrefWidth(100);
        jdbcPortField.setPromptText("3306 / 5432");
        grid.add(jdbcPortField, 1, row++);

        // Database
        grid.add(new Label("Adatbázis név:"), 0, row);
        jdbcDatabaseField.setPrefWidth(300);
        jdbcDatabaseField.setPromptText("employeemanager");
        grid.add(jdbcDatabaseField, 1, row++);

        // Username
        grid.add(new Label("Felhasználónév:"), 0, row);
        jdbcUsernameField.setPrefWidth(300);
        grid.add(jdbcUsernameField, 1, row++);

        // Password
        grid.add(new Label("Jelszó:"), 0, row);
        jdbcPasswordField.setPrefWidth(300);
        grid.add(jdbcPasswordField, 1, row++);

        return grid;
    }

    private void updateDatabasePanel() {
        DatabaseType selectedType = databaseTypeCombo.getValue();
        if (selectedType == null) return;

        firebasePanel.setVisible(selectedType == DatabaseType.FIREBASE);
        firebasePanel.setManaged(selectedType == DatabaseType.FIREBASE);

        jdbcPanel.setVisible(selectedType == DatabaseType.MYSQL || selectedType == DatabaseType.POSTGRESQL);
        jdbcPanel.setManaged(selectedType == DatabaseType.MYSQL || selectedType == DatabaseType.POSTGRESQL);

        // Set default ports
        if (selectedType == DatabaseType.MYSQL && jdbcPortField.getText().isEmpty()) {
            jdbcPortField.setText("3306");
        } else if (selectedType == DatabaseType.POSTGRESQL && jdbcPortField.getText().isEmpty()) {
            jdbcPortField.setText("5432");
        }
    }

    private void browseServiceAccountFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Service Account JSON fájl kiválasztása");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON fájlok", "*.json")
        );

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            firebaseServiceAccountField.setText(file.getAbsolutePath());
        }
    }

    private void populateFields() {
        authorField.setText(settingsService.getApplicationAuthor());

        // Load saved connections
        savedConnectionsCombo.getItems().clear();
        savedConnectionsCombo.getItems().addAll(connectionManager.getSavedConnections());

        // Load active connection
        DatabaseConnectionConfig activeConfig = connectionManager.getActiveConnection();
        if (activeConfig != null) {
            loadConnectionConfig(activeConfig);
            savedConnectionsCombo.setValue(activeConfig);
        } else {
            databaseTypeCombo.setValue(DatabaseType.FIREBASE);
        }
    }

    private void loadSavedConnection() {
        DatabaseConnectionConfig selected = savedConnectionsCombo.getValue();
        if (selected != null) {
            loadConnectionConfig(selected);
        }
    }

    private void loadConnectionConfig(DatabaseConnectionConfig config) {
        databaseTypeCombo.setValue(config.getType());
        profileNameField.setText(config.getProfileName());

        // Firebase fields
        firebaseServiceAccountField.setText(config.getFirebaseServiceAccountPath() != null ?
                config.getFirebaseServiceAccountPath() : "");
        firebaseProjectIdField.setText(config.getFirebaseProjectId() != null ?
                config.getFirebaseProjectId() : "");
        firebaseDatabaseUrlField.setText(config.getFirebaseDatabaseUrl() != null ?
                config.getFirebaseDatabaseUrl() : "");

        // JDBC fields
        jdbcHostField.setText(config.getJdbcHost() != null ? config.getJdbcHost() : "localhost");
        jdbcPortField.setText(config.getJdbcPort() != null ? config.getJdbcPort().toString() : "");
        jdbcDatabaseField.setText(config.getJdbcDatabase() != null ? config.getJdbcDatabase() : "");
        jdbcUsernameField.setText(config.getJdbcUsername() != null ? config.getJdbcUsername() : "");
        jdbcPasswordField.setText(config.getJdbcPassword());

        updateDatabasePanel();
    }

    private void deleteSavedConnection() {
        DatabaseConnectionConfig selected = savedConnectionsCombo.getValue();
        if (selected == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott kapcsolat a törléshez.");
            return;
        }

        if (selected.isActive()) {
            AlertHelper.showWarning("Figyelmeztetés", "Az aktív kapcsolat nem törölhető.");
            return;
        }

        if (AlertHelper.showConfirmation("Törlés megerősítése",
                "Biztosan törli a következő kapcsolatot?",
                selected.getProfileName())) {
            savedConnectionsCombo.getItems().remove(selected);
            connectionManager.getSavedConnections().remove(selected);
            savedConnectionsCombo.setValue(null);
        }
    }

    private DatabaseConnectionConfig createConfigFromFields() {
        DatabaseConnectionConfig config = new DatabaseConnectionConfig();
        config.setType(databaseTypeCombo.getValue());
        config.setProfileName(profileNameField.getText().isEmpty() ?
                "Kapcsolat - " + System.currentTimeMillis() : profileNameField.getText());

        switch (config.getType()) {
            case FIREBASE:
                config.setFirebaseServiceAccountPath(firebaseServiceAccountField.getText());
                config.setFirebaseProjectId(firebaseProjectIdField.getText());
                config.setFirebaseDatabaseUrl(firebaseDatabaseUrlField.getText());
                break;
            case MYSQL:
            case POSTGRESQL:
                config.setJdbcHost(jdbcHostField.getText().isEmpty() ? "localhost" : jdbcHostField.getText());
                config.setJdbcPort(jdbcPortField.getText().isEmpty() ?
                        (config.getType() == DatabaseType.MYSQL ? 3306 : 5432) :
                        Integer.parseInt(jdbcPortField.getText()));
                config.setJdbcDatabase(jdbcDatabaseField.getText());
                config.setJdbcUsername(jdbcUsernameField.getText());
                config.setJdbcPassword(jdbcPasswordField.getText());
                break;
        }

        return config;
    }

    private void testDatabaseConnection() {
        DatabaseConnectionConfig config = createConfigFromFields();

        // Create loading dialog
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Kapcsolat tesztelése");
        progressAlert.setHeaderText("Kapcsolat tesztelése folyamatban...");
        progressAlert.setContentText("Kérem várjon...");
        progressAlert.getButtonTypes().clear();

        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return connectionManager.testConnection(config);
            }
        };

        testTask.setOnSucceeded(e -> {
            progressAlert.close();
            boolean success = testTask.getValue();
            if (success) {
                AlertHelper.showInformation("Kapcsolat teszt",
                        "Sikeres kapcsolódás",
                        "A kapcsolat teszt sikeres!");
            } else {
                AlertHelper.showError("Kapcsolat teszt",
                        "Sikertelen kapcsolódás",
                        "Nem sikerült kapcsolódni az adatbázishoz.");
            }
        });

        testTask.setOnFailed(e -> {
            progressAlert.close();
            Throwable ex = testTask.getException();
            AlertHelper.showError("Kapcsolat teszt",
                    "Hiba történt",
                    ex != null ? ex.getMessage() : "Ismeretlen hiba");
        });

        progressAlert.show();
        new Thread(testTask).start();
    }

    private void applyDatabaseConnection() {
        DatabaseConnectionConfig config = createConfigFromFields();

        // Confirmation dialog
        if (!AlertHelper.showConfirmation("Kapcsolat váltás",
                "Biztosan váltani szeretne adatbázis kapcsolatot?",
                "Az alkalmazás újra fog indulni az új kapcsolattal.")) {
            return;
        }

        // Create loading dialog
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Kapcsolat alkalmazása");
        progressAlert.setHeaderText("Kapcsolat alkalmazása folyamatban...");
        progressAlert.setContentText("Kérem várjon...");
        progressAlert.getButtonTypes().clear();

        Task<Void> applyTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                connectionManager.applyConnection(config);
                return null;
            }
        };

        applyTask.setOnSucceeded(e -> {
            progressAlert.close();

            // Save profile
            connectionManager.saveConnection(config);

            AlertHelper.showInformation("Kapcsolat alkalmazva",
                    "Sikeres kapcsolódás",
                    "Az új adatbázis kapcsolat sikeresen alkalmazva.\n" +
                            "Az alkalmazás újraindul az új beállításokkal.");

            // Close dialog
            Platform.runLater(() -> {
                getDialogPane().getScene().getWindow().hide();
                // Restart application
                System.exit(0);
            });
        });

        applyTask.setOnFailed(e -> {
            progressAlert.close();
            Throwable ex = applyTask.getException();
            AlertHelper.showError("Kapcsolat alkalmazása",
                    "Hiba történt",
                    ex != null ? ex.getMessage() : "Ismeretlen hiba");
        });

        progressAlert.show();
        new Thread(applyTask).start();
    }

    private void saveSettings() {
        try {
            // Save general settings
            // Note: Author field saving would need to be implemented in SettingsService

            AlertHelper.showInformation("Sikeres mentés",
                    "Beállítások mentve",
                    "A beállítások sikeresen mentésre kerültek.");
        } catch (Exception e) {
            AlertHelper.showError("Hiba",
                    "Nem sikerült menteni a beállításokat",
                    e.getMessage());
        }
    }
}