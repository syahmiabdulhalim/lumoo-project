package com.example.lumoo.infrastructure.db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
public class FullTextIndexInitializer {
    private static final Logger log = LoggerFactory.getLogger(FullTextIndexInitializer.class);
    private final JdbcTemplate jdbc;
    public FullTextIndexInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    @EventListener(ApplicationReadyEvent.class)
    public void createFullTextIndex() {
        try {
            Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                "WHERE table_schema = DATABASE() AND table_name = 'products' AND index_name = 'ft_product_name'",
                Long.class);
            if (count != null && count == 0) {
                jdbc.execute("ALTER TABLE products ADD FULLTEXT INDEX ft_product_name (name)");
                log.info("FULLTEXT index ft_product_name created on products.name");
            }
        } catch (Exception e) {
            log.warn("Could not create FULLTEXT index (non-fatal, LIKE search still works): {}", e.getMessage());
        }
    }
}
