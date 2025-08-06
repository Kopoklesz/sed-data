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

import java.io.File;

public class SettingsDialog extends Dialog<Void> {

    private final SettingsService settingsService;
    private final DatabaseConnectionManager connectionManager;

    // Existing connections tab components
    private ListView<DatabaseConnectionConfig> connectionsList;
    private Label connectionDetailsLabel;
    private Button connectButton;
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

    // Status components
    private Label statusLabel;
    private ProgressIndicator progressIndicator;

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
        dialogPane.setPrefWidth(700);
        dialogPane.setPrefHeight(650);

        // Apply custom CSS
        dialogPane.getStylesheets().add(getClass().getResource("/css/settings-dialog.css").toExternalForm());
        dialogPane.getStyleClass().add("settings-dialog");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Existing connections tab
        Tab existingConnectionsTab = new Tab("📋 Meglévő kapcsolatok");
        existingConnectionsTab.setContent(createExistingConnectionsPanel());

        // New connection tab
        Tab newConnectionTab = new Tab("➕ Új kapcsolat");
        newConnectionTab.setContent(createNewConnectionPanel());

        tabPane.getTabs().addAll(existingConnectionsTab, newConnectionTab);

        // Status area at bottom
        VBox mainContainer = new VBox(10);
        mainContainer.getChildren().addAll(tabPane, createStatusArea());

        dialogPane.setContent(mainContainer);
    }

    private VBox createExistingConnectionsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #f8f9fa;");

        // Header
        Label headerLabel = new Label("🗄️ Mentett adatbázis kapcsolatok");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        headerLabel.setTextFill(Color.web("#2196F3"));

        // Connections list
        connectionsList = new ListView<>();
        connectionsList.setPrefHeight(300);
        connectionsList.setCellFactory(listView -> new ConnectionListCell());
        connectionsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateConnectionDetails(newVal)
        );

        // Connection details
        connectionDetailsLabel = new Label("Válasszon ki egy kapcsolatot a részletekért");
        connectionDetailsLabel.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10;");
        connectionDetailsLabel.setWrapText(true);
        connectionDetailsLabel.setPrefHeight(100);
        connectionDetailsLabel.setAlignment(Pos.TOP_LEFT);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        connectButton = new Button("🔌 Kapcsolódás");
        connectButton.setPrefWidth(180);
        connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");
        connectButton.setDisable(true);
        connectButton.setOnAction(e -> connectToSelectedDatabase());

        deleteConnectionButton = new Button("🗑️ Törlés");
        deleteConnectionButton.setPrefWidth(180);
        deleteConnectionButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");
        deleteConnectionButton.setDisable(true);
        deleteConnectionButton.setOnAction(e -> deleteSelectedConnection());

        buttonBox.getChildren().addAll(connectButton, deleteConnectionButton);

        panel.getChildren().addAll(
                headerLabel,
                new Label("Válasszon a mentett kapcsolatok közül:"),
                connectionsList,
                new Label("Kapcsolat részletei:"),
                connectionDetailsLabel,
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

        VBox content = new VBox(20);

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

        // Action buttons
        HBox actionButtons = createNewConnectionButtons();

        content.getChildren().addAll(
                headerLabel,
                nameBox,
                typeSelector,
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

    private VBox createNameBox() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15;");
        box.setEffect(new javafx.scene.effect.DropShadow(5, Color.gray(0.3)));

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

        Label headerLabel = new Label("🔥 Firebase Beállítások");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setTextFill(Color.web("#ff6f00"));

        // Service account file
        VBox fileBox = new VBox(5);
        Label fileLabel = new Label("Service Account JSON fájl:");
        fileLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        HBox fileInputBox = new HBox(10);
        firebaseServiceAccountField.setPrefWidth(350);
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
        grid.add(dbLabel, 0, 1);
        grid.add(jdbcDatabaseField, 1, 1, 3, 1);

        // Username
        Label userLabel = new Label("Felhasználónév:");
        userLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        jdbcUsernameField.setPromptText("adatbázis felhasználó");
        grid.add(userLabel, 0, 2);
        grid.add(jdbcUsernameField, 1, 2, 3, 1);

        // Password
        Label passLabel = new Label("Jelszó:");
        passLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        jdbcPasswordField.setPromptText("••••••••");
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

    private HBox createStatusArea() {
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);

        statusLabel = new Label("Készen áll");
        statusLabel.setFont(Font.font(13));

        statusBox.getChildren().addAll(progressIndicator, statusLabel);
        return statusBox;
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

    private void updateConnectionDetails(DatabaseConnectionConfig config) {
        if (config == null) {
            connectionDetailsLabel.setText("Válasszon ki egy kapcsolatot a részletekért");
            connectButton.setDisable(true);
            deleteConnectionButton.setDisable(true);
        } else {
            StringBuilder details = new StringBuilder();
            details.append("Név: ").append(config.getProfileName()).append("\n");
            details.append("Típus: ").append(config.getType().getDisplayName()).append("\n");
            details.append("Aktív: ").append(config.isActive() ? "✅ Igen" : "❌ Nem").append("\n\n");

            switch (config.getType()) {
                case FIREBASE:
                    details.append("Project ID: ").append(config.getFirebaseProjectId()).append("\n");
                    details.append("Database URL: ").append(config.getFirebaseDatabaseUrl());
                    break;
                case MYSQL:
                case POSTGRESQL:
                    details.append("Host: ").append(config.getJdbcHost()).append("\n");
                    details.append("Port: ").append(config.getJdbcPort()).append("\n");
                    details.append("Adatbázis: ").append(config.getJdbcDatabase()).append("\n");
                    details.append("Felhasználó: ").append(config.getJdbcUsername());
                    break;
            }

            connectionDetailsLabel.setText(details.toString());
            connectButton.setDisable(config.isActive());
            deleteConnectionButton.setDisable(config.isActive());
        }
    }

    private void connectToSelectedDatabase() {
        DatabaseConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!AlertHelper.showConfirmation("Kapcsolat váltás",
                "Biztosan váltani szeretne erre a kapcsolatra?",
                selected.getProfileName() + "\n\nAz alkalmazás újra fog indulni.")) {
            return;
        }

        updateStatus("🔄 Kapcsolódás...", Color.BLUE);
        progressIndicator.setVisible(true);

        Task<Void> connectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                connectionManager.applyConnection(selected);
                return null;
            }
        };

        connectTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                updateStatus("✅ Kapcsolat létrehozva! Az alkalmazás újraindul...", Color.GREEN);

                Task<Void> restartTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        Thread.sleep(2000);
                        return null;
                    }
                };

                restartTask.setOnSucceeded(event -> {
                    Platform.runLater(() -> {
                        getDialogPane().getScene().getWindow().hide();
                        System.exit(0);
                    });
                });

                new Thread(restartTask).start();
            });
        });

        connectTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                Throwable ex = connectTask.getException();
                updateStatus("❌ Hiba: " + (ex != null ? ex.getMessage() : "Ismeretlen hiba"), Color.RED);
            });
        });

        new Thread(connectTask).start();
    }

    private void deleteSelectedConnection() {
        DatabaseConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.isActive()) {
            updateStatus("❌ Az aktív kapcsolat nem törölhető.", Color.RED);
            return;
        }

        if (AlertHelper.showConfirmation("Törlés megerősítése",
                "Biztosan törli ezt a kapcsolatot?",
                selected.getProfileName())) {
            connectionsList.getItems().remove(selected);
            connectionManager.getSavedConnections().remove(selected);
            connectionManager.saveConnection(selected); // This will remove it from saved file
            updateStatus("✅ Kapcsolat törölve.", Color.GREEN);
            loadConnections();
        }
    }

    private void testNewConnection() {
        if (profileNameField.getText().trim().isEmpty()) {
            updateStatus("⚠️ Adjon meg egy nevet a kapcsolatnak!", Color.ORANGE);
            return;
        }

        DatabaseConnectionConfig config = createConfigFromFields();

        updateStatus("🔄 Kapcsolat tesztelése...", Color.BLUE);
        progressIndicator.setVisible(true);

        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return connectionManager.testConnection(config);
            }
        };

        testTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                boolean success = testTask.getValue();
                if (success) {
                    updateStatus("✅ Sikeres kapcsolódás!", Color.GREEN);
                } else {
                    updateStatus("❌ Sikertelen kapcsolódás.", Color.RED);
                }
            });
        });

        testTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                Throwable ex = testTask.getException();
                updateStatus("❌ Hiba: " + (ex != null ? ex.getMessage() : "Ismeretlen hiba"), Color.RED);
            });
        });

        new Thread(testTask).start();
    }

    private void saveNewConnection() {
        if (profileNameField.getText().trim().isEmpty()) {
            updateStatus("⚠️ Adjon meg egy nevet a kapcsolatnak!", Color.ORANGE);
            return;
        }

        DatabaseConnectionConfig config = createConfigFromFields();

        // Check if name already exists
        boolean nameExists = connectionManager.getSavedConnections().stream()
                .anyMatch(c -> c.getProfileName().equals(config.getProfileName()));

        if (nameExists) {
            updateStatus("⚠️ Már létezik kapcsolat ezzel a névvel!", Color.ORANGE);
            return;
        }

        updateStatus("💾 Kapcsolat mentése...", Color.BLUE);
        progressIndicator.setVisible(true);

        Task<Void> saveTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                connectionManager.saveConnection(config);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                updateStatus("✅ Kapcsolat elmentve!", Color.GREEN);
                clearNewConnectionFields();
                loadConnections();

                // Switch to existing connections tab
                TabPane tabPane = (TabPane) getDialogPane().getContent().lookup(".tab-pane");
                if (tabPane != null) {
                    tabPane.getSelectionModel().selectFirst();
                }
            });
        });

        saveTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                updateStatus("❌ Hiba a mentés során!", Color.RED);
            });
        });

        new Thread(saveTask).start();
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

    private void updateStatus(String message, Color color) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        });
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