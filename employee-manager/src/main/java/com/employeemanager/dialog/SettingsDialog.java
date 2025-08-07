package com.employeemanager.dialog;

import com.employeemanager.config.DatabaseConnectionConfig;
import com.employeemanager.config.DatabaseConnectionManager;
import com.employeemanager.config.DatabaseType;
import com.employeemanager.service.impl.SettingsService;
import com.employeemanager.util.AlertHelper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SettingsDialog extends Dialog<Void> {

    private final SettingsService settingsService;
    private final DatabaseConnectionManager connectionManager;

    // Existing connections tab components
    private ListView<DatabaseConnectionConfig> connectionsList;
    private Button connectButton;
    private Button editConnectionButton;
    private Button deleteConnectionButton;

    // New connection tab components
    private final ComboBox<DatabaseType> databaseTypeCombo = new ComboBox<>();
    private final TextField profileNameField = new TextField();

    // Firebase fields
    private final TextField firebaseServiceAccountField = new TextField();
    private final Button firebaseBrowseButton = new Button("📁 Tallózás");
    private final TextField firebaseProjectIdField = new TextField();
    private final TextField firebaseDatabaseUrlField = new TextField();

    // JDBC fields
    private final TextField jdbcHostField = new TextField();
    private final TextField jdbcPortField = new TextField();
    private final TextField jdbcDatabaseField = new TextField();
    private final TextField jdbcUsernameField = new TextField();
    private final PasswordField jdbcPasswordField = new PasswordField();

    // Dynamic panels
    private VBox firebasePanel;
    private VBox jdbcPanel;

    // Tab pane reference
    private TabPane mainTabPane;
    private Tab newConnectionTab;

    // Edit mode flag
    private boolean isEditMode = false;
    private DatabaseConnectionConfig editingConfig = null;

    public SettingsDialog(SettingsService settingsService, DatabaseConnectionManager connectionManager) {
        this.settingsService = settingsService;
        this.connectionManager = connectionManager;

        setTitle("Adatbázis kapcsolat kezelő");
        setHeaderText(null);

        setupDialog();
        loadConnections();
        updateDatabasePanel();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);
        dialogPane.setPrefWidth(900);  // Növelt szélesség
        dialogPane.setPrefHeight(700); // Növelt magasság

        // Apply custom CSS
        dialogPane.getStylesheets().add(getClass().getResource("/css/settings-dialog.css").toExternalForm());
        dialogPane.getStyleClass().add("settings-dialog");

        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Existing connections tab
        Tab existingConnectionsTab = new Tab("📋 Meglévő kapcsolatok");
        existingConnectionsTab.setContent(createExistingConnectionsPanel());

        // New connection tab
        newConnectionTab = new Tab("➕ Új kapcsolat");
        newConnectionTab.setContent(createNewConnectionPanel());

        mainTabPane.getTabs().addAll(existingConnectionsTab, newConnectionTab);

        dialogPane.setContent(mainTabPane);
    }

    private VBox createExistingConnectionsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #f8f9fa;");

        // Header
        Label headerLabel = new Label("🗄️ Mentett adatbázis kapcsolatok");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        headerLabel.setTextFill(Color.web("#2196F3"));

        // Info label
        Label infoLabel = new Label("Válasszon a mentett kapcsolatok közül:");
        infoLabel.setFont(Font.font(14));

        // Connections list
        connectionsList = new ListView<>();
        connectionsList.setPrefHeight(400); // Növelt magasság
        connectionsList.setCellFactory(listView -> new ConnectionListCell());
        connectionsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateButtonStates(newVal)
        );

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        connectButton = new Button("🔌 Kapcsolódás");
        connectButton.setPrefWidth(180);
        connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");
        connectButton.setDisable(true);
        connectButton.setOnAction(e -> connectToSelectedDatabase());

        editConnectionButton = new Button("✏️ Szerkesztés");
        editConnectionButton.setPrefWidth(180);
        editConnectionButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");
        editConnectionButton.setDisable(true);
        editConnectionButton.setOnAction(e -> editSelectedConnection());

        deleteConnectionButton = new Button("❌ Törlés");
        deleteConnectionButton.setPrefWidth(180);
        deleteConnectionButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");
        deleteConnectionButton.setDisable(true);
        deleteConnectionButton.setOnAction(e -> deleteSelectedConnection());

        buttonBox.getChildren().addAll(connectButton, editConnectionButton, deleteConnectionButton);

        panel.getChildren().addAll(
                headerLabel,
                infoLabel,
                connectionsList,
                buttonBox
        );

        return panel;
    }

    private VBox createNewConnectionPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #f8f9fa;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setPadding(new Insets(0, 10, 0, 0)); // Jobb oldali padding hogy ne lógjon ki

        VBox content = new VBox(20);
        content.setPadding(new Insets(0, 10, 0, 0)); // Extra padding a tartalom számára

        // Header
        Label headerLabel = new Label("➕ Új adatbázis kapcsolat létrehozása");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        headerLabel.setTextFill(Color.web("#2196F3"));

        // Connection name
        VBox nameBox = createNameBox();

        // Database type selector
        VBox typeSelector = createTypeSelector();

        // Connection panels
        firebasePanel = createFirebasePanel();
        jdbcPanel = createJdbcPanel();

        // Test connection templates
        HBox templateButtons = createTemplateButtons();

        // Action buttons
        HBox actionButtons = createNewConnectionButtons();

        content.getChildren().addAll(
                headerLabel,
                nameBox,
                typeSelector,
                templateButtons,
                new Separator(),
                firebasePanel,
                jdbcPanel,
                new Separator(),
                actionButtons
        );

        scrollPane.setContent(content);
        panel.getChildren().add(scrollPane);

        return panel;
    }

    private HBox createTemplateButtons() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 0, 10, 0));

        Button mysqlTemplateBtn = new Button("🐬 Docker MySQL teszt kitöltése");
        mysqlTemplateBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold;");
        mysqlTemplateBtn.setOnAction(e -> fillMySQLTemplate());

        Button postgresTemplateBtn = new Button("🐘 Docker PostgreSQL teszt kitöltése");
        postgresTemplateBtn.setStyle("-fx-background-color: #607d8b; -fx-text-fill: white; -fx-font-weight: bold;");
        postgresTemplateBtn.setOnAction(e -> fillPostgreSQLTemplate());

        box.getChildren().addAll(mysqlTemplateBtn, postgresTemplateBtn);
        return box;
    }

    private void fillMySQLTemplate() {
        profileNameField.setText("Docker MySQL Test");
        databaseTypeCombo.setValue(DatabaseType.MYSQL);
        jdbcHostField.setText("localhost");
        jdbcPortField.setText("3306");
        jdbcDatabaseField.setText("testdb");
        jdbcUsernameField.setText("testuser");
        jdbcPasswordField.setText("testpass");
        updateDatabasePanel();
    }

    private void fillPostgreSQLTemplate() {
        profileNameField.setText("Docker PostgreSQL Test");
        databaseTypeCombo.setValue(DatabaseType.POSTGRESQL);
        jdbcHostField.setText("localhost");
        jdbcPortField.setText("5432");
        jdbcDatabaseField.setText("testdb");
        jdbcUsernameField.setText("testuser");
        jdbcPasswordField.setText("testpass");
        updateDatabasePanel();
    }

    private VBox createNameBox() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15;");
        box.setEffect(new javafx.scene.effect.DropShadow(5, Color.gray(0.3)));
        box.setMaxWidth(850); // Max szélesség beállítása

        Label label = new Label("📝 Kapcsolat neve:");
        label.setFont(Font.font("System", FontWeight.BOLD, 14));

        profileNameField.setPromptText("pl. Teszt szerver, Éles adatbázis, Fejlesztői környezet");
        profileNameField.setStyle("-fx-font-size: 14px;");

        box.getChildren().addAll(label, profileNameField);
        return box;
    }

    private VBox createTypeSelector() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15;");
        box.setEffect(new javafx.scene.effect.DropShadow(5, Color.gray(0.3)));
        box.setMaxWidth(850);

        Label label = new Label("Adatbázis típus:");
        label.setFont(Font.font("System", FontWeight.BOLD, 14));

        databaseTypeCombo.getItems().addAll(DatabaseType.values());
        databaseTypeCombo.setPrefWidth(Double.MAX_VALUE);
        databaseTypeCombo.setStyle("-fx-font-size: 14px;");
        databaseTypeCombo.setValue(DatabaseType.FIREBASE);

        // Custom cell factory for icons
        databaseTypeCombo.setCellFactory(combo -> new ListCell<DatabaseType>() {
            @Override
            protected void updateItem(DatabaseType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getIconForType(item) + " " + item.getDisplayName());
                }
            }
        });

        databaseTypeCombo.setButtonCell(new ListCell<DatabaseType>() {
            @Override
            protected void updateItem(DatabaseType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getIconForType(item) + " " + item.getDisplayName());
                }
            }
        });

        databaseTypeCombo.setOnAction(e -> updateDatabasePanel());

        box.getChildren().addAll(label, databaseTypeCombo);
        return box;
    }

    private VBox createFirebasePanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 10; -fx-padding: 15;");
        panel.setEffect(new javafx.scene.effect.DropShadow(5, Color.gray(0.3)));
        panel.setMaxWidth(850);

        Label headerLabel = new Label("🔥 Firebase Beállítások");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setTextFill(Color.web("#ff6f00"));

        // Service account file
        VBox fileBox = new VBox(5);
        Label fileLabel = new Label("Service Account JSON fájl:");
        fileLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        HBox fileInputBox = new HBox(10);
        firebaseServiceAccountField.setPrefWidth(650);
        firebaseServiceAccountField.setPromptText("Válassza ki a JSON fájlt...");
        firebaseBrowseButton.setStyle("-fx-background-color: #ff6f00; -fx-text-fill: white; -fx-font-weight: bold;");
        firebaseBrowseButton.setOnAction(e -> browseServiceAccountFile());

        fileInputBox.getChildren().addAll(firebaseServiceAccountField, firebaseBrowseButton);
        fileBox.getChildren().addAll(fileLabel, fileInputBox);

        // Project ID
        VBox projectBox = new VBox(5);
        Label projectLabel = new Label("Project ID:");
        projectLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        firebaseProjectIdField.setPromptText("pl. employee-manager-12345");
        projectBox.getChildren().addAll(projectLabel, firebaseProjectIdField);

        // Database URL
        VBox urlBox = new VBox(5);
        Label urlLabel = new Label("Database URL:");
        urlLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        firebaseDatabaseUrlField.setPromptText("https://your-project.firebaseio.com");
        urlBox.getChildren().addAll(urlLabel, firebaseDatabaseUrlField);

        panel.getChildren().addAll(headerLabel, fileBox, projectBox, urlBox);
        return panel;
    }

    private VBox createJdbcPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 10; -fx-padding: 15;");
        panel.setEffect(new javafx.scene.effect.DropShadow(5, Color.gray(0.3)));
        panel.setMaxWidth(850);

        Label headerLabel = new Label("🗄️ Adatbázis Kapcsolat Beállítások");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setTextFill(Color.web("#1976d2"));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);

        // Host
        Label hostLabel = new Label("Host:");
        hostLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        jdbcHostField.setPromptText("localhost vagy szerver címe");
        jdbcHostField.setText("localhost");
        jdbcHostField.setPrefWidth(300);
        grid.add(hostLabel, 0, 0);
        grid.add(jdbcHostField, 1, 0);

        // Port
        Label portLabel = new Label("Port:");
        portLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        jdbcPortField.setPromptText("3306 / 5432");
        jdbcPortField.setPrefWidth(100);
        grid.add(portLabel, 2, 0);
        grid.add(jdbcPortField, 3, 0);

        // Database
        Label dbLabel = new Label("Adatbázis:");
        dbLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        jdbcDatabaseField.setPromptText("employeemanager");
        jdbcDatabaseField.setPrefWidth(400);
        grid.add(dbLabel, 0, 1);
        grid.add(jdbcDatabaseField, 1, 1, 3, 1);

        // Username
        Label userLabel = new Label("Felhasználónév:");
        userLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        jdbcUsernameField.setPromptText("adatbázis felhasználó");
        jdbcUsernameField.setPrefWidth(400);
        grid.add(userLabel, 0, 2);
        grid.add(jdbcUsernameField, 1, 2, 3, 1);

        // Password
        Label passLabel = new Label("Jelszó:");
        passLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        jdbcPasswordField.setPromptText("••••••••");
        jdbcPasswordField.setPrefWidth(400);
        grid.add(passLabel, 0, 3);
        grid.add(jdbcPasswordField, 1, 3, 3, 1);

        panel.getChildren().addAll(headerLabel, grid);
        return panel;
    }

    private HBox createNewConnectionButtons() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 10, 0));

        Button testButton = new Button("🔍 Kapcsolat tesztelése");
        testButton.setPrefWidth(180);
        testButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");
        testButton.setOnAction(e -> testNewConnection());

        Button saveButton = new Button("💾 Mentés");
        saveButton.setPrefWidth(180);
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");
        saveButton.setOnAction(e -> saveNewConnection());

        buttonBox.getChildren().addAll(testButton, saveButton);
        return buttonBox;
    }

    private void updateButtonStates(DatabaseConnectionConfig config) {
        if (config == null) {
            connectButton.setDisable(true);
            editConnectionButton.setDisable(true);
            deleteConnectionButton.setDisable(true);
        } else {
            connectButton.setDisable(config.isActive());
            editConnectionButton.setDisable(false);
            deleteConnectionButton.setDisable(config.isActive());
        }
    }

    private void editSelectedConnection() {
        DatabaseConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Switch to new connection tab
        mainTabPane.getSelectionModel().select(newConnectionTab);

        // Enter edit mode
        isEditMode = true;
        editingConfig = selected;

        // Change tab title
        newConnectionTab.setText("✏️ Kapcsolat szerkesztése");

        // Fill fields with selected connection data
        profileNameField.setText(selected.getProfileName());
        databaseTypeCombo.setValue(selected.getType());

        switch (selected.getType()) {
            case FIREBASE:
                firebaseServiceAccountField.setText(selected.getFirebaseServiceAccountPath());
                firebaseProjectIdField.setText(selected.getFirebaseProjectId());
                firebaseDatabaseUrlField.setText(selected.getFirebaseDatabaseUrl());
                break;
            case MYSQL:
            case POSTGRESQL:
                jdbcHostField.setText(selected.getJdbcHost());
                jdbcPortField.setText(String.valueOf(selected.getJdbcPort()));
                jdbcDatabaseField.setText(selected.getJdbcDatabase());
                jdbcUsernameField.setText(selected.getJdbcUsername());
                jdbcPasswordField.setText(selected.getJdbcPassword());
                break;
        }

        updateDatabasePanel();
    }

    private void cancelEdit() {
        isEditMode = false;
        editingConfig = null;
        newConnectionTab.setText("➕ Új kapcsolat");
        clearNewConnectionFields();
        mainTabPane.getSelectionModel().selectFirst();
    }

    private void connectToSelectedDatabase() {
        DatabaseConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!AlertHelper.showConfirmation("Kapcsolat váltás",
                "Biztosan váltani szeretne erre a kapcsolatra?",
                selected.getProfileName() + "\n\n" +
                        "⚠️ Az alkalmazás újra fog indulni a kapcsolat váltásához!")) {
            return;
        }

        // Create a progress dialog
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Kapcsolódás");
        progressAlert.setHeaderText("Kapcsolat váltás folyamatban...");
        progressAlert.setContentText("Az alkalmazás újraindul...");
        progressAlert.getButtonTypes().clear();

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        progressAlert.setGraphic(progressIndicator);

        Task<Boolean> connectTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // Apply the connection (this saves it as active)
                    connectionManager.applyConnection(selected);
                    return true;
                } catch (Exception e) {
                    throw e;
                }
            }
        };

        connectTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                try {
                    progressAlert.close();
                } catch (Exception ex) {
                    // Ignore
                }

                // Show success message briefly
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Sikeres kapcsolódás");
                successAlert.setHeaderText("Kapcsolat létrehozva!");
                successAlert.setContentText("Az alkalmazás most újraindul...\n" +
                        "Új kapcsolat: " + selected.getProfileName());
                successAlert.show();

                // Restart application after delay
                Task<Void> restartTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        Thread.sleep(2000);
                        return null;
                    }
                };

                restartTask.setOnSucceeded(event -> {
                    Platform.runLater(() -> {
                        try {
                            // Close all windows
                            getDialogPane().getScene().getWindow().hide();
                            successAlert.close();

                            // Restart the application
                            restartApplication();
                        } catch (Exception ex) {
                            log.error("Error during restart: {}", ex.getMessage());
                        }
                    });
                });

                new Thread(restartTask).start();
            });
        });

        connectTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                try {
                    progressAlert.close();
                } catch (Exception ex) {
                    // Ignore
                }
                Throwable ex = connectTask.getException();
                AlertHelper.showError("Kapcsolódási hiba",
                        "Nem sikerült kapcsolódni",
                        ex != null ? ex.getMessage() : "Ismeretlen hiba");
            });
        });

        progressAlert.show();
        new Thread(connectTask).start();
    }

    private void restartApplication() {
        try {
            // Get the command that started this application
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String currentJar = new File(SettingsDialog.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();

            // Build command
            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-jar");
            command.add(currentJar);

            // Start new instance
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();

            // Exit current instance
            System.exit(0);

        } catch (Exception e) {
            log.error("Failed to restart application: {}", e.getMessage());
            // Fallback: just exit and let user restart manually
            AlertHelper.showInformation("Újraindítás szükséges",
                    "Kérem indítsa újra az alkalmazást",
                    "A kapcsolat váltás érvénybe lépéséhez kérem indítsa újra manuálisan az alkalmazást.");
            System.exit(0);
        }
    }

    private void deleteSelectedConnection() {
        DatabaseConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.isActive()) {
            AlertHelper.showWarning("Törlés nem lehetséges",
                    "Az aktív kapcsolat nem törölhető.");
            return;
        }

        if (AlertHelper.showConfirmation("Törlés megerősítése",
                "Biztosan törli ezt a kapcsolatot?",
                selected.getProfileName())) {
            connectionManager.deleteConnection(selected);
            loadConnections();
            AlertHelper.showInformation("Sikeres törlés",
                    "Kapcsolat törölve",
                    selected.getProfileName() + " sikeresen törölve.");
        }
    }

    private void testNewConnection() {
        if (profileNameField.getText().trim().isEmpty()) {
            AlertHelper.showWarning("Hiányzó adat",
                    "Adjon meg egy nevet a kapcsolatnak!");
            return;
        }

        DatabaseConnectionConfig config = createConfigFromFields();

        // Create a progress dialog
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Kapcsolat tesztelése");
        progressAlert.setHeaderText("Tesztelés folyamatban...");
        progressAlert.setContentText("Kapcsolódás: " + config.getType().getDisplayName() +
                "\nHost: " + (config.getJdbcHost() != null ? config.getJdbcHost() : "N/A"));
        progressAlert.getButtonTypes().clear();

        // Add cancel button
        ButtonType cancelButton = new ButtonType("Mégse", ButtonBar.ButtonData.CANCEL_CLOSE);
        progressAlert.getButtonTypes().add(cancelButton);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        progressAlert.setGraphic(progressIndicator);

        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Set timeout of 15 seconds for test
                Thread currentThread = Thread.currentThread();
                Thread timeoutThread = new Thread(() -> {
                    try {
                        Thread.sleep(15000); // 15 seconds timeout
                        if (!currentThread.isInterrupted()) {
                            currentThread.interrupt();
                            cancel();
                        }
                    } catch (InterruptedException ignored) {}
                });
                timeoutThread.setDaemon(true);
                timeoutThread.start();

                try {
                    return connectionManager.testConnection(config);
                } finally {
                    timeoutThread.interrupt();
                }
            }
        };

        testTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                try {
                    progressAlert.close();
                } catch (Exception ex) {
                    // Ignore close errors
                }
                boolean success = testTask.getValue();
                if (success) {
                    AlertHelper.showInformation("Teszt sikeres",
                            "Kapcsolat teszt",
                            "Sikeres kapcsolódás az adatbázishoz!\n" +
                                    "Típus: " + config.getType().getDisplayName() + "\n" +
                                    (config.getType() != DatabaseType.FIREBASE ?
                                            "Host: " + config.getJdbcHost() + ":" + config.getJdbcPort() + "\n" +
                                                    "Adatbázis: " + config.getJdbcDatabase() :
                                            "Project: " + config.getFirebaseProjectId()));
                } else {
                    AlertHelper.showError("Teszt sikertelen",
                            "Kapcsolat teszt",
                            "Nem sikerült kapcsolódni az adatbázishoz.\n" +
                                    "Ellenőrizze a kapcsolódási adatokat!");
                }
            });
        });

        testTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                try {
                    progressAlert.close();
                } catch (Exception ex) {
                    // Ignore close errors
                }
                Throwable ex = testTask.getException();
                AlertHelper.showError("Teszt hiba",
                        "Kapcsolat teszt",
                        "Hiba történt a tesztelés során:\n" +
                                (ex != null ? ex.getMessage() : "Ismeretlen hiba"));
            });
        });

        testTask.setOnCancelled(e -> {
            Platform.runLater(() -> {
                try {
                    progressAlert.close();
                } catch (Exception ex) {
                    // Ignore close errors
                }
                AlertHelper.showWarning("Megszakítva",
                        "A tesztelés megszakítva vagy időtúllépés történt.");
            });
        });

        // Handle dialog cancel button
        progressAlert.setOnCloseRequest(event -> {
            testTask.cancel(true);
        });

        progressAlert.show();
        new Thread(testTask).start();
    }

    private void saveNewConnection() {
        if (profileNameField.getText().trim().isEmpty()) {
            AlertHelper.showWarning("Hiányzó adat",
                    "Adjon meg egy nevet a kapcsolatnak!");
            return;
        }

        DatabaseConnectionConfig config = createConfigFromFields();

        // Check if name already exists (except in edit mode)
        if (!isEditMode) {
            boolean nameExists = connectionManager.getSavedConnections().stream()
                    .anyMatch(c -> c.getProfileName().equals(config.getProfileName()));

            if (nameExists) {
                AlertHelper.showWarning("Duplikált név",
                        "Már létezik kapcsolat ezzel a névvel!");
                return;
            }
        }

        Task<Void> saveTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (isEditMode) {
                    connectionManager.updateConnection(config);
                } else {
                    connectionManager.saveConnection(config);
                }
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                AlertHelper.showInformation("Sikeres mentés",
                        isEditMode ? "Kapcsolat módosítva" : "Kapcsolat elmentve",
                        config.getProfileName());

                if (isEditMode) {
                    cancelEdit();
                } else {
                    clearNewConnectionFields();
                }

                loadConnections();
                mainTabPane.getSelectionModel().selectFirst();
            });
        });

        saveTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                AlertHelper.showError("Mentési hiba",
                        "Nem sikerült menteni",
                        "Hiba történt a mentés során.");
            });
        });

        new Thread(saveTask).start();
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

    private void loadConnections() {
        connectionsList.getItems().clear();
        connectionsList.getItems().addAll(connectionManager.getSavedConnections());

        // Select active connection if exists
        DatabaseConnectionConfig activeConfig = connectionManager.getActiveConnection();
        if (activeConfig != null) {
            connectionsList.getSelectionModel().select(activeConfig);
        }
    }

    private DatabaseConnectionConfig createConfigFromFields() {
        DatabaseConnectionConfig config = new DatabaseConnectionConfig();
        config.setType(databaseTypeCombo.getValue());
        config.setProfileName(profileNameField.getText().trim());

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

    private void clearNewConnectionFields() {
        profileNameField.clear();
        firebaseServiceAccountField.clear();
        firebaseProjectIdField.clear();
        firebaseDatabaseUrlField.clear();
        jdbcHostField.setText("localhost");
        jdbcPortField.clear();
        jdbcDatabaseField.clear();
        jdbcUsernameField.clear();
        jdbcPasswordField.clear();
        databaseTypeCombo.setValue(DatabaseType.FIREBASE);
    }

    private String getIconForType(DatabaseType type) {
        return switch (type) {
            case FIREBASE -> "🔥";
            case MYSQL -> "🐬";
            case POSTGRESQL -> "🐘";
        };
    }

    // Custom list cell for connections list
    private class ConnectionListCell extends ListCell<DatabaseConnectionConfig> {
        @Override
        protected void updateItem(DatabaseConnectionConfig item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox box = new HBox(10);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(5));

                Label iconLabel = new Label(getIconForType(item.getType()));
                iconLabel.setFont(Font.font(20));

                VBox textBox = new VBox(2);
                Label nameLabel = new Label(item.getProfileName());
                nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

                Label typeLabel = new Label(item.getType().getDisplayName());
                typeLabel.setFont(Font.font(12));
                typeLabel.setTextFill(Color.GRAY);

                textBox.getChildren().addAll(nameLabel, typeLabel);

                if (item.isActive()) {
                    Label activeLabel = new Label("✅ Aktív");
                    activeLabel.setFont(Font.font(12));
                    activeLabel.setTextFill(Color.GREEN);
                    box.getChildren().addAll(iconLabel, textBox, activeLabel);
                } else {
                    box.getChildren().addAll(iconLabel, textBox);
                }

                setGraphic(box);
            }
        }
    }
}