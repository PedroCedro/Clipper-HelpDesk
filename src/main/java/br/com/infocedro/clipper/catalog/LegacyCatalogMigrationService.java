package br.com.infocedro.clipper.catalog;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executa uma ponte idempotente; o banco legado permanece somente leitura. */
@Service
public class LegacyCatalogMigrationService {

    private static final String SOURCE_TYPE = "totvs-winthor";
    private static final String SELECT_ARTICLES = """
            select id, url, titulo, secao_id, secao_nome, secao_caminho,
                   labels_json, conteudo_texto, conteudo_html, criado_em,
                   atualizado_em, source_api, collected_at
              from totvs_winthor_articles
             order by id
            """;

    private final RawKnowledgeDocumentRepository repository;
    private final RawDocumentFingerprint fingerprint;
    private final LegacyTotvsDocumentMapper mapper;

    public LegacyCatalogMigrationService(
            RawKnowledgeDocumentRepository repository,
            RawDocumentFingerprint fingerprint,
            LegacyTotvsDocumentMapper mapper) {
        this.repository = repository;
        this.fingerprint = fingerprint;
        this.mapper = mapper;
    }

    @Transactional
    public LegacyCatalogMigrationResult migrate(Path databasePath) throws SQLException {
        Map<String, RawDocumentIdentityProjection> existing = new HashMap<>();
        repository.findIdentitiesBySourceType(SOURCE_TYPE)
                .forEach(document -> existing.put(document.getExternalId(), document));

        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        OffsetDateTime now = OffsetDateTime.now();
        String jdbcUrl = "jdbc:h2:" + databasePath.toAbsolutePath().normalize()
                + ";IFEXISTS=TRUE;ACCESS_MODE_DATA=r";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(SELECT_ARTICLES)) {
            while (rows.next()) {
                RawDocumentCandidate candidate = mapper.map(readRow(rows));
                String hash = fingerprint.calculate(candidate);
                RawDocumentIdentityProjection identity = existing.get(candidate.externalId());
                if (identity == null) {
                    RawKnowledgeDocument created = RawKnowledgeDocument.create(candidate, hash, now);
                    repository.save(created);
                    inserted++;
                } else if (!identity.getContentHash().equals(hash)) {
                    RawKnowledgeDocument current = repository.findById(identity.getId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Documento desapareceu durante a migração: " + identity.getId()));
                    current.apply(candidate, hash, now);
                    updated++;
                } else {
                    unchanged++;
                }
            }
        }
        return new LegacyCatalogMigrationResult(inserted, updated, unchanged);
    }

    private LegacyTotvsDocumentMapper.LegacyRow readRow(ResultSet rows) throws SQLException {
        return new LegacyTotvsDocumentMapper.LegacyRow(
                rows.getLong("id"), rows.getString("url"), rows.getString("titulo"),
                rows.getLong("secao_id"), rows.getString("secao_nome"), rows.getString("secao_caminho"),
                rows.getString("labels_json"), rows.getString("conteudo_texto"), rows.getString("conteudo_html"),
                rows.getString("criado_em"), rows.getString("atualizado_em"), rows.getString("source_api"),
                rows.getString("collected_at"));
    }
}
