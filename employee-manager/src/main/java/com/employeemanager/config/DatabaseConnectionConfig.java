package com.employeemanager.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConnectionConfig {
    private DatabaseType type = DatabaseType.FIREBASE;

    // Firebase settings
    private String firebaseServiceAccountPath;
    private String firebaseProjectId;
    private String firebaseDatabaseUrl;

    // MySQL/PostgreSQL settings
    private String jdbcHost;
    private Integer jdbcPort;
    private String jdbcDatabase;
    private String jdbcUsername;
    private String jdbcPasswordEncrypted;

    // Common settings
    private String profileName;
    private boolean active;

    private static final String ENCRYPTION_KEY = "Empl0yeeM@nager!"; // 16 chars for AES

    @JsonIgnore
    public String getJdbcPassword() {
        if (jdbcPasswordEncrypted == null || jdbcPasswordEncrypted.isEmpty()) {
            return "";
        }
        try {
            return decrypt(jdbcPasswordEncrypted);
        } catch (Exception e) {
            return "";
        }
    }

    public void setJdbcPassword(String password) {
        if (password == null || password.isEmpty()) {
            this.jdbcPasswordEncrypted = "";
            return;
        }
        try {
            this.jdbcPasswordEncrypted = encrypt(password);
        } catch (Exception e) {
            this.jdbcPasswordEncrypted = "";
        }
    }

    @JsonIgnore
    public String getJdbcUrl() {
        if (type == DatabaseType.MYSQL) {
            return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                    jdbcHost != null ? jdbcHost : "localhost",
                    jdbcPort != null ? jdbcPort : 3306,
                    jdbcDatabase != null ? jdbcDatabase : "employeemanager");
        } else if (type == DatabaseType.POSTGRESQL) {
            return String.format("jdbc:postgresql://%s:%d/%s",
                    jdbcHost != null ? jdbcHost : "localhost",
                    jdbcPort != null ? jdbcPort : 5432,
                    jdbcDatabase != null ? jdbcDatabase : "employeemanager");
        }
        return null;
    }

    private String encrypt(String strToEncrypt) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
    }

    private String decrypt(String strToDecrypt) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
    }

    public DatabaseConnectionConfig copy() {
        DatabaseConnectionConfig copy = new DatabaseConnectionConfig();
        copy.type = this.type;
        copy.firebaseServiceAccountPath = this.firebaseServiceAccountPath;
        copy.firebaseProjectId = this.firebaseProjectId;
        copy.firebaseDatabaseUrl = this.firebaseDatabaseUrl;
        copy.jdbcHost = this.jdbcHost;
        copy.jdbcPort = this.jdbcPort;
        copy.jdbcDatabase = this.jdbcDatabase;
        copy.jdbcUsername = this.jdbcUsername;
        copy.jdbcPasswordEncrypted = this.jdbcPasswordEncrypted;
        copy.profileName = this.profileName;
        copy.active = this.active;
        return copy;
    }
}