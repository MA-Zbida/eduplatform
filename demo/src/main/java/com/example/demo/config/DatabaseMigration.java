package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Database migration component that ensures new columns exist.
 * This runs on application startup to add any missing columns.
 */
@Component
public class DatabaseMigration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        log.info("Running database migrations...");
        
        addColumnIfNotExists("enrollments", "course_completed", "BOOLEAN DEFAULT FALSE");
        addColumnIfNotExists("enrollments", "course_completed_at", "TIMESTAMP");
        addColumnIfNotExists("courses", "module_id", "BIGINT");
        addColumnIfNotExists("courses", "display_order", "INTEGER DEFAULT 0");
        
        // PDF support columns
        addColumnIfNotExists("courses", "content_type", "VARCHAR(255) DEFAULT 'TEXT'");
        addColumnIfNotExists("courses", "pdf_filename", "VARCHAR(255)");
        addColumnIfNotExists("courses", "pdf_original_name", "VARCHAR(255)");
        
        // Create modules table if it doesn't exist
        createModulesTableIfNotExists();
        
        log.info("Database migrations completed.");
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        try {
            String checkSql = """
                SELECT COUNT(*) FROM information_schema.columns 
                WHERE table_name = ? AND column_name = ?
            """;
            
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, columnName);
            
            if (count == null || count == 0) {
                String alterSql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition);
                jdbcTemplate.execute(alterSql);
                log.info("Added column {} to table {}", columnName, tableName);
            }
        } catch (Exception e) {
            log.warn("Could not add column {} to {}: {}", columnName, tableName, e.getMessage());
        }
    }

    private void createModulesTableIfNotExists() {
        try {
            String checkSql = """
                SELECT COUNT(*) FROM information_schema.tables 
                WHERE table_name = 'modules'
            """;
            
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (count == null || count == 0) {
                String createSql = """
                    CREATE TABLE modules (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        display_order INTEGER DEFAULT 0,
                        active BOOLEAN DEFAULT TRUE,
                        created_by VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """;
                jdbcTemplate.execute(createSql);
                log.info("Created modules table");
            }
        } catch (Exception e) {
            log.warn("Could not create modules table: {}", e.getMessage());
        }
    }
}
