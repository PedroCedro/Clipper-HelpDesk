package br.com.infocedro.clipper.collector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

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

        public void upsertSection(Map<String, Object> record) throws SQLException {
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
                statement.setLong(1, asLong(record.get("id")));
                statement.setString(2, asString(record.get("nome")));
                statement.setString(3, asString(record.get("url")));
                statement.setLong(4, asLong(record.get("category_id")));
                setNullableLong(statement, 5, record.get("parent_section_id"));
                statement.setString(6, asString(record.get("caminho")));
                statement.setString(7, asString(record.get("collected_at")));
                statement.executeUpdate();
            }
        }

        public void upsertArticle(Map<String, Object> record) throws SQLException, JsonProcessingException {
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
                statement.setLong(1, asLong(record.get("id")));
                statement.setString(2, asString(record.get("url")));
                statement.setString(3, asString(record.get("titulo")));
                statement.setLong(4, asLong(record.get("secao_id")));
                statement.setString(5, asString(record.get("secao_nome")));
                statement.setString(6, asString(record.get("secao_caminho")));
                statement.setString(7, objectMapper.writeValueAsString(record.get("labels")));
                statement.setString(8, asString(record.get("conteudo_texto")));
                statement.setString(9, asString(record.get("conteudo_html")));
                statement.setString(10, asString(record.get("criado_em")));
                statement.setString(11, asString(record.get("atualizado_em")));
                statement.setString(12, asString(record.get("source_api")));
                statement.setString(13, asString(record.get("collected_at")));
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

        private static String asString(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }
}
