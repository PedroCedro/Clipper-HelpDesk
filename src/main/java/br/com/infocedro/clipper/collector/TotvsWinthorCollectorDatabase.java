package br.com.infocedro.clipper.collector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

// Persistência opcional do crawler em H2. Os JSONL continuam sendo gerados;
// o banco facilita consultas locais e reexecuções incrementais por MERGE.
@Service
public class TotvsWinthorCollectorDatabase {

    private final ObjectMapper objectMapper;

    public TotvsWinthorCollectorDatabase(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DatabaseSession open(TotvsWinthorCollectorProperties properties) throws SQLException, IOException {
        if (!properties.isDatabaseEnabled()) {
            return DatabaseSession.disabled();
        }

        Path databasePath = properties.getDatabasePath().toAbsolutePath().normalize();
        Path parent = databasePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Connection connection = DriverManager.getConnection("jdbc:h2:file:" + databasePath, "sa", "");
        DatabaseSession session = new DatabaseSession(connection, objectMapper);
        session.initialize();
        return session;
    }

    public static class DatabaseSession implements AutoCloseable {

        private final Connection connection;
        private final ObjectMapper objectMapper;

        private DatabaseSession(Connection connection, ObjectMapper objectMapper) {
            this.connection = connection;
            this.objectMapper = objectMapper;
        }

        private static DatabaseSession disabled() {
            // Null Object: o crawler pode chamar upsert/close sem espalhar ifs.
            return new DatabaseSession(null, null);
        }

        private void initialize() throws SQLException {
            if (connection == null) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS totvs_winthor_sections (
                            id BIGINT PRIMARY KEY,
                            nome VARCHAR(512) NOT NULL,
                            url VARCHAR(2048),
                            category_id BIGINT,
                            parent_section_id BIGINT,
                            caminho CLOB,
                            collected_at VARCHAR(64)
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS totvs_winthor_articles (
                            id BIGINT PRIMARY KEY,
                            url VARCHAR(2048),
                            titulo CLOB NOT NULL,
                            secao_id BIGINT,
                            secao_nome VARCHAR(512),
                            secao_caminho CLOB,
                            labels_json CLOB,
                            conteudo_texto CLOB,
                            conteudo_html CLOB,
                            criado_em VARCHAR(64),
                            atualizado_em VARCHAR(64),
                            source_api VARCHAR(2048),
                            collected_at VARCHAR(64)
                        )
                        """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_totvs_winthor_articles_secao ON totvs_winthor_articles(secao_id)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_totvs_winthor_articles_atualizado ON totvs_winthor_articles(atualizado_em)");
            }
        }

        public void upsertSection(TotvsSectionRecord record) throws SQLException {
            if (connection == null) {
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    MERGE INTO totvs_winthor_sections (
                        id, nome, url, category_id, parent_section_id, caminho, collected_at
                    )
                    KEY(id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setLong(1, record.id());
                statement.setString(2, record.name());
                statement.setString(3, record.url());
                statement.setLong(4, record.categoryId());
                setNullableLong(statement, 5, record.parentSectionId());
                statement.setString(6, record.path());
                statement.setString(7, record.collectedAt());
                statement.executeUpdate();
            }
        }

        public void upsertArticle(TotvsArticleRecord record) throws SQLException, JsonProcessingException {
            if (connection == null) {
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    MERGE INTO totvs_winthor_articles (
                        id, url, titulo, secao_id, secao_nome, secao_caminho, labels_json,
                        conteudo_texto, conteudo_html, criado_em, atualizado_em, source_api, collected_at
                    )
                    KEY(id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setLong(1, record.id());
                statement.setString(2, record.url());
                statement.setString(3, record.title());
                statement.setLong(4, record.sectionId());
                statement.setString(5, record.sectionName());
                statement.setString(6, record.sectionPath());
                statement.setString(7, objectMapper.writeValueAsString(record.labels()));
                statement.setString(8, record.textContent());
                statement.setString(9, record.htmlContent());
                statement.setString(10, record.createdAt());
                statement.setString(11, record.updatedAt());
                statement.setString(12, record.sourceApi());
                statement.setString(13, record.collectedAt());
                statement.executeUpdate();
            }
        }

        @Override
        public void close() throws SQLException {
            if (connection != null) {
                connection.close();
            }
        }

        private static void setNullableLong(PreparedStatement statement, int index, Object value) throws SQLException {
            if (value == null) {
                statement.setObject(index, null);
                return;
            }
            statement.setLong(index, asLong(value));
        }

        private static long asLong(Object value) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(String.valueOf(value));
        }

    }
}
