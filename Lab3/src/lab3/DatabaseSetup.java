package lab3;

import java.sql.*;
import java.util.Random;

public class DatabaseSetup {

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS benchmark_data (" +
            "id SERIAL PRIMARY KEY, " +
            "name VARCHAR(255) NOT NULL, " +
            "value DOUBLE PRECISION NOT NULL" +
            ")";

    public static void createTable(String dbUrl, String dbUser, String dbPassword) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(CREATE_TABLE_SQL);
            ConsoleLogger.logMain("Table 'benchmark_data' created/verified.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create table: " + e.getMessage(), e);
        }
    }

    public static void populateTable(String dbUrl, String dbUser, String dbPassword, int recordCount) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement truncateStmt = conn.createStatement();
             PreparedStatement insertStmt = conn.prepareStatement(
                     "INSERT INTO benchmark_data (name, value) VALUES (?, ?)")) {

            truncateStmt.executeUpdate("TRUNCATE TABLE benchmark_data RESTART IDENTITY");

            Random rng = new Random(42);
            for (int i = 0; i < recordCount; i++) {
                insertStmt.setString(1, "record_" + (i + 1));
                insertStmt.setDouble(2, rng.nextDouble() * 1000.0);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
            ConsoleLogger.logMain("Populated table with " + recordCount + " records.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to populate table: " + e.getMessage(), e);
        }
    }

    public static void resetTable(String dbUrl, String dbUser, String dbPassword, int recordCount) {
        populateTable(dbUrl, dbUser, dbPassword, recordCount);
    }

    public static void createDatabase(String dbUrl, String dbUser, String dbPassword) {
        String baseUrl = dbUrl.substring(0, dbUrl.lastIndexOf('/') + 1) + "postgres";
        String dbName = dbUrl.substring(dbUrl.lastIndexOf('/') + 1);

        try (Connection conn = DriverManager.getConnection(baseUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
            if (!rs.next()) {
                stmt.executeUpdate("CREATE DATABASE " + dbName);
                ConsoleLogger.logMain("Database '" + dbName + "' created.");
            } else {
                ConsoleLogger.logMain("Database '" + dbName + "' already exists.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database: " + e.getMessage(), e);
        }
    }
}
