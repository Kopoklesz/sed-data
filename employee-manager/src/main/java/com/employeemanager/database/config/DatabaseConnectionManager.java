package com.employeemanager.database.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
// import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adatbázis kapcsolatok központi kezelője
 */
@Slf4j
@Component
public class DatabaseConnectionManager {
    
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    private final Map<String, Firestore> firestoreCache = new ConcurrentHashMap<>();
    private ConnectionConfig activeConnection;
    
    /**
     * Kapcsolat tesztelése
     */
    public boolean testConnection(ConnectionConfig config) {
        if (!config.isValid()) {
            log.error("Invalid connection configuration");
            return false;
        }
        
        switch (config.getType()) {
            case FIREBASE:
                return testFirebaseConnection(config);
            case MYSQL:
            case POSTGRESQL:
            case H2:
                return testJdbcConnection(config);
            default:
                return false;
        }
    }
    
    /**
     * JDBC kapcsolat tesztelése
     */
    private boolean testJdbcConnection(ConnectionConfig config) {
        String jdbcUrl = config.getJdbcUrl();
        log.info("Testing JDBC connection to: {}", jdbcUrl);
        
        try {
            // Driver betöltése
            Class.forName(config.getDriverClassName());
            
            // Kapcsolódás
            try (Connection conn = DriverManager.getConnection(
                    jdbcUrl, 
                    config.getUsername(), 
                    config.getPassword())) {
                
                return conn != null && !conn.isClosed();
            }
        } catch (ClassNotFoundException e) {
            log.error("JDBC driver not found: {}", config.getDriverClassName(), e);
            return false;
        } catch (SQLException e) {
            log.error("Failed to connect to database: {}", jdbcUrl, e);
            return false;
        }
    }
    
    /**
     * Firebase kapcsolat tesztelése
     */
    private boolean testFirebaseConnection(ConnectionConfig config) {
        try {
            // Service account betöltése
            Resource serviceAccount = loadFirebaseServiceAccount(config);
            if (!serviceAccount.exists()) {
                log.error("Firebase service account not found: {}", 
                    config.getFirebaseServiceAccountPath());
                return false;
            }
            
            // Kapcsolat tesztelése
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(
                    serviceAccount.getInputStream()))
                .setProjectId(config.getFirebaseProjectId())
                .setDatabaseUrl(config.getFirebaseDatabaseUrl())
                .build();
            
            // Ideiglenes app létrehozása teszteléshez
            String appName = "test-" + System.currentTimeMillis();
            FirebaseApp testApp = FirebaseApp.initializeApp(options, appName);
            
            try {
                Firestore firestore = FirestoreClient.getFirestore(testApp);
                // Egyszerű teszt lekérdezés
                firestore.collection("test").limit(1).get().get();
                return true;
            } finally {
                testApp.delete();
            }
            
        } catch (Exception e) {
            log.error("Failed to connect to Firebase", e);
            return false;
        }
    }
    
    /**
     * DataSource létrehozása vagy cache-ből visszaadása
     */
    public DataSource getDataSource(ConnectionConfig config) {
        if (config.getType() == DatabaseType.FIREBASE) {
            throw new IllegalArgumentException("Use getFirestore() for Firebase connections");
        }
        
        String cacheKey = getCacheKey(config);
        return dataSourceCache.computeIfAbsent(cacheKey, key -> createDataSource(config));
    }
    
    /**
     * HikariCP DataSource létrehozása
     */
    private DataSource createDataSource(ConnectionConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriverClassName());
        
        // Connection pool beállítások
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setPoolName("EmployeeManager-" + config.getName());
        
        // Adatbázis specifikus beállítások
        if (config.getType() == DatabaseType.MYSQL) {
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }
        
        log.info("Creating DataSource for: {}", config.getName());
        return new HikariDataSource(hikariConfig);
    }
    
    /**
     * Firestore instance létrehozása vagy cache-ből visszaadása
     */
    public Firestore getFirestore(ConnectionConfig config) throws IOException {
        if (config.getType() != DatabaseType.FIREBASE) {
            throw new IllegalArgumentException("Use getDataSource() for SQL connections");
        }
        
        String cacheKey = getCacheKey(config);
        return firestoreCache.computeIfAbsent(cacheKey, key -> {
            try {
                return createFirestore(config);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create Firestore", e);
            }
        });
    }
    
    /**
     * Firestore létrehozása
     */
    private Firestore createFirestore(ConnectionConfig config) throws IOException {
        Resource serviceAccount = loadFirebaseServiceAccount(config);
        
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
            .setProjectId(config.getFirebaseProjectId())
            .setDatabaseUrl(config.getFirebaseDatabaseUrl())
            .build();
        
        String appName = "app-" + config.getName();
        
        // Ha már létezik ilyen nevű app, töröljük
        try {
            FirebaseApp existingApp = FirebaseApp.getInstance(appName);
            existingApp.delete();
        } catch (IllegalStateException e) {
            // Nincs ilyen app, OK
        }
        
        FirebaseApp app = FirebaseApp.initializeApp(options, appName);
        log.info("Created Firebase app: {}", appName);
        
        return FirestoreClient.getFirestore(app);
    }
    
    /**
     * Firebase service account betöltése
     */
    private Resource loadFirebaseServiceAccount(ConnectionConfig config) {
        String path = config.getFirebaseServiceAccountPath();
        
        if (path.startsWith("classpath:")) {
            return new ClassPathResource(path.substring("classpath:".length()));
        } else {
            return new FileSystemResource(path);
        }
    }
    
    /**
     * Cache kulcs generálása
     */
    private String getCacheKey(ConnectionConfig config) {
        return config.getType() + ":" + config.getName();
    }
    
    /**
     * Aktív kapcsolat beállítása
     */
    public void setActiveConnection(ConnectionConfig config) {
        this.activeConnection = config;
        log.info("Active database connection set to: {} ({})", 
            config.getName(), config.getType());
    }
    
    /**
     * Aktív kapcsolat lekérése
     */
    public ConnectionConfig getActiveConnection() {
        return activeConnection;
    }
    
    /**
     * Összes kapcsolat bezárása és cache tisztítása
     */
    public void closeAll() {
        // DataSource-ok bezárása
        dataSourceCache.values().forEach(ds -> {
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
            }
        });
        dataSourceCache.clear();
        
        // Firebase app-ok törlése
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
        firestoreCache.clear();
        
        log.info("All database connections closed");
    }
}