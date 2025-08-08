package com.employeemanager.dialog;

import com.employeemanager.config.DatabaseConnectionManager;
import com.employeemanager.config.DatabaseType;
import com.employeemanager.repository.RepositoryFactory;
import com.employeemanager.service.impl.DatabaseSwitchService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Dialog to show current database connection status
 */
public class DatabaseStatusDialog extends Dialog<Void> {

    private final DatabaseConnectionManager connectionManager;
    private final RepositoryFactory repositoryFactory;
    private final DatabaseSwitchService switchService;

    public DatabaseStatusDialog(DatabaseConnectionManager connectionManager,
                                RepositoryFactory repositoryFactory,
                                DatabaseSwitchService switchService) {
        this.connectionManager = connectionManager;
        this.repositoryFactory = repositoryFactory;
        this.switchService = switchService;

        setTitle("Adatbázis állapot");
        setHeaderText("Aktuális adatbázis kapcsolat információk");

        setupDialog();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);
        dialogPane.setPrefWidth(600);
        dialogPane.setPrefHeight(400);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Current connection section
        VBox connectionSection = createConnectionSection();

        // Repository status section
        VBox repositorySection = createRepositorySection();

        // System status section
        VBox systemSection = createSystemSection();

        content.getChildren().addAll(
                connectionSection,
                new Separator(),
                repositorySection,
                new Separator(),
                systemSection
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        dialogPane.setContent(scrollPane);
    }

    private VBox createConnectionSection() {
        VBox section = new VBox(10);

        Label titleLabel = new Label("🔌 Aktív kapcsolat");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 0, 0, 20));

        int row = 0;

        if (connectionManager.getActiveConnection() != null) {
            var config = connectionManager.getActiveConnection();

            addStatusRow(grid, row++, "Profil név:", config.getProfileName());
            addStatusRow(grid, row++, "Típus:", config.getType().getDisplayName());

            // Type-specific information
            switch (config.getType()) {
                case FIREBASE:
                    addStatusRow(grid, row++, "Project ID:", config.getFirebaseProjectId());
                    addStatusRow(grid, row++, "Database URL:", config.getFirebaseDatabaseUrl());
                    addStatusRow(grid, row++, "Firestore állapot:",
                            connectionManager.getCurrentFirestore() != null ?
                                    "✅ Kapcsolódva" : "❌ Nincs kapcsolat");
                    break;

                case MYSQL:
                case POSTGRESQL:
                    addStatusRow(grid, row++, "Host:", config.getJdbcHost() + ":" + config.getJdbcPort());
                    addStatusRow(grid, row++, "Adatbázis:", config.getJdbcDatabase());
                    addStatusRow(grid, row++, "Felhasználó:", config.getJdbcUsername());
                    addStatusRow(grid, row++, "DataSource állapot:",
                            connectionManager.getCurrentDataSource() != null ?
                                    "✅ Kapcsolódva" : "❌ Nincs kapcsolat");

                    // Try to get connection pool stats
                    if (connectionManager.getCurrentDataSource() != null) {
                        try {
                            var ds = connectionManager.getCurrentDataSource();
                            // This would work with HikariCP
                            if (ds instanceof com.zaxxer.hikari.HikariDataSource) {
                                var hikari = (com.zaxxer.hikari.HikariDataSource) ds;
                                addStatusRow(grid, row++, "Pool méret:",
                                        hikari.getHikariPoolMXBean().getActiveConnections() + "/" +
                                                hikari.getMaximumPoolSize());
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    break;
            }
        } else {
            Label noConnectionLabel = new Label("Nincs aktív kapcsolat");
            noConnectionLabel.setTextFill(Color.RED);
            grid.add(noConnectionLabel, 0, row, 2, 1);
        }

        section.getChildren().addAll(titleLabel, grid);
        return section;
    }

    private VBox createRepositorySection() {
        VBox section = new VBox(10);

        Label titleLabel = new Label("📦 Repository állapot");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 0, 0, 20));

        int row = 0;

        addStatusRow(grid, row++, "Aktuális típus:",
                repositoryFactory.getCurrentType() != null ?
                        repositoryFactory.getCurrentType().toString() : "N/A");

        addStatusRow(grid, row++, "Frissítés alatt:",
                repositoryFactory.isUpdating() ? "⏳ Igen" : "✅ Nem");

        addStatusRow(grid, row++, "Employee Repository:",
                repositoryFactory.getEmployeeRepository() != null ?
                        "✅ Elérhető" : "❌ Nem elérhető");

        addStatusRow(grid, row++, "WorkRecord Repository:",
                repositoryFactory.getWorkRecordRepository() != null ?
                        "✅ Elérhető" : "❌ Nem elérhető");

        section.getChildren().addAll(titleLabel, grid);
        return section;
    }

    private VBox createSystemSection() {
        VBox section = new VBox(10);

        Label titleLabel = new Label("⚙️ Rendszer állapot");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 0, 0, 20));

        int row = 0;

        // Database switch service status
        if (switchService != null) {
            var status = switchService.getDatabaseStatus();
            addStatusRow(grid, row++, "Szolgáltatás állapot:", status.toString());
            addStatusRow(grid, row++, "Váltás folyamatban:",
                    switchService.isSwitching() ? "⏳ Igen" : "✅ Nem");
        }

        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        addStatusRow(grid, row++, "Memória használat:",
                String.format("%d MB / %d MB", usedMemory, maxMemory));

        // Thread count
        addStatusRow(grid, row++, "Aktív szálak:",
                String.valueOf(Thread.activeCount()));

        section.getChildren().addAll(titleLabel, grid);
        return section;
    }

    private void addStatusRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setFont(Font.font("System", FontWeight.BOLD, 12));

        Label valueNode = new Label(value != null ? value : "N/A");
        valueNode.setFont(Font.font("System", 12));

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }
}