package br.com.infocedro.clipper.catalog;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/** Hash determinístico dos campos efetivamente persistidos e pesquisados. */
@Component
public class RawDocumentFingerprint {

    public String calculate(RawDocumentCandidate candidate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, candidate.sourceType());
            add(digest, candidate.externalId());
            add(digest, candidate.scope());
            add(digest, candidate.module());
            add(digest, candidate.title());
            add(digest, candidate.textContent());
            add(digest, candidate.htmlContent());
            add(digest, candidate.sourceUrl());
            add(digest, candidate.labelsText());
            add(digest, candidate.routinesText());
            add(digest, candidate.errorCodesText());
            add(digest, candidate.metadataJson());
            add(digest, String.valueOf(candidate.sourceCreatedAt()));
            add(digest, String.valueOf(candidate.sourceUpdatedAt()));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", exception);
        }
    }

    private void add(MessageDigest digest, String value) {
        byte[] bytes = (value != null ? value : "").getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
