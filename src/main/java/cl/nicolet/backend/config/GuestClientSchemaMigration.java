package cl.nicolet.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

@Component
public class GuestClientSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GuestClientSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public GuestClientSchemaMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String database = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (!database.contains("mysql")) {
                return;
            }
        }

        jdbcTemplate.execute("ALTER TABLE cliente MODIFY COLUMN usuario_id BIGINT NULL");
        log.info("Esquema de clientes actualizado para permitir perfiles de invitados");
    }
}
