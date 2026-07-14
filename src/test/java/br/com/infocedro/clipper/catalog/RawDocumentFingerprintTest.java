package br.com.infocedro.clipper.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class RawDocumentFingerprintTest {

    @Test
    void ehDeterministicoEMudaComTextoDerivado() {
        RawDocumentFingerprint fingerprint = new RawDocumentFingerprint();
        RawDocumentCandidate original = candidate("texto um");
        RawDocumentCandidate changed = candidate("texto dois");

        assertEquals(fingerprint.calculate(original), fingerprint.calculate(original));
        assertNotEquals(fingerprint.calculate(original), fingerprint.calculate(changed));
    }

    private RawDocumentCandidate candidate(String text) {
        return new RawDocumentCandidate(
                "manual", "1", "geral", null, "Título", text, "<p>x</p>", null,
                "", "", "", "{}", null, null, OffsetDateTime.now());
    }
}
