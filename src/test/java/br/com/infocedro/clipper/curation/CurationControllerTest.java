package br.com.infocedro.clipper.curation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurationController.class)
class CurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurationCaseService caseService;
    @MockitoBean
    private CurationCandidateService candidateService;
    @MockitoBean
    private CurationQueryService queryService;

    @Test
    void criaCasoComAtorEDevolveLocation() throws Exception {
        CurationCaseSnapshot snapshot = snapshot(12L, CurationStatus.ABERTO);
        when(caseService.create(new CreateCurationCaseCommand(
                CurationOriginType.TICKET, "ticket-9", "pedro", "Recorrência")))
                .thenReturn(snapshot);

        mockMvc.perform(post("/api/curation-cases")
                        .header("X-Actor", "pedro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originType":"TICKET","originReference":"ticket-9","reason":"Recorrência"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/curation-cases/12"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist());
    }

    @Test
    void listaCasosFiltradosComContagemReal() throws Exception {
        when(queryService.list(CurationStatus.COM_CANDIDATOS)).thenReturn(List.of(
                new CurationCaseSummary(
                        12L, CurationOriginType.MANUAL, null, CurationStatus.COM_CANDIDATOS,
                        "Investigar", "pedro", 0, now(), now())));

        mockMvc.perform(get("/api/curation-cases").param("status", "COM_CANDIDATOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidateCount").value(0));
        verify(queryService).list(CurationStatus.COM_CANDIDATOS);
    }

    @Test
    void detalheExpoeCandidatosEHistoricosSemEntidades() throws Exception {
        when(queryService.detail(12L)).thenReturn(new CurationCaseDetail(
                12L, CurationOriginType.MANUAL, null, CurationStatus.COM_CANDIDATOS,
                "Investigar", "pedro", 1, now(), now(),
                List.of(new CurationCandidateView(1L, 77L, "pedro", "Relevante", now())),
                List.of(new CurationTransitionView(
                        2L, CurationStatus.ABERTO, CurationStatus.COM_CANDIDATOS,
                        "pedro", "Relevante", now())),
                List.of(new CurationCandidateEventView(
                        3L, 77L, CurationCandidateEventType.ADDED, "pedro", "Relevante", now()))));

        mockMvc.perform(get("/api/curation-cases/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].documentId").value(77))
                .andExpect(jsonPath("$.transitions[0].toStatus").value("COM_CANDIDATOS"))
                .andExpect(jsonPath("$.candidateEvents[0].eventType").value("ADDED"))
                .andExpect(jsonPath("$.candidates[0].curationCase").doesNotExist());
    }

    @Test
    void associaERemoveCandidatoComAtorDeclarado() throws Exception {
        CurationCandidateChangeResult changed = new CurationCandidateChangeResult(
                12L, 77L, true, 1, CurationStatus.COM_CANDIDATOS);
        when(candidateService.add(
                12L, 77L, new ChangeCurationCandidateCommand("pedro", "Relevante")))
                .thenReturn(changed);
        when(candidateService.remove(
                12L, 77L, new ChangeCurationCandidateCommand("pedro", "Insuficiente")))
                .thenReturn(new CurationCandidateChangeResult(
                        12L, 77L, true, 0, CurationStatus.COM_CANDIDATOS));

        mockMvc.perform(post("/api/curation-cases/12/candidates")
                        .header("X-Actor", "pedro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":77,\"reason\":\"Relevante\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1));

        mockMvc.perform(delete("/api/curation-cases/12/candidates/77")
                        .header("X-Actor", "pedro")
                        .param("reason", "Insuficiente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }

    @Test
    void transicaoInvalidaRetorna400() throws Exception {
        TransitionCurationCaseCommand command = new TransitionCurationCaseCommand(
                CurationStatus.PUBLICADO, "pedro", "Publicar");
        when(caseService.transition(12L, command))
                .thenThrow(new InvalidCurationTransitionException("Publicação bloqueada"));

        mockMvc.perform(post("/api/curation-cases/12/transitions")
                        .header("X-Actor", "pedro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"PUBLICADO\",\"reason\":\"Publicar\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void operacaoMutavelSemAtorRetorna400() throws Exception {
        mockMvc.perform(post("/api/curation-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originType\":\"MANUAL\",\"reason\":\"Investigar\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/curation-cases")
                        .header("X-Actor", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originType\":\"MANUAL\",\"reason\":\"Investigar\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void casoInexistenteRetorna404() throws Exception {
        when(queryService.detail(404L)).thenThrow(new CurationCaseNotFoundException(404L));

        mockMvc.perform(get("/api/curation-cases/404"))
                .andExpect(status().isNotFound());
    }

    private CurationCaseSnapshot snapshot(Long id, CurationStatus status) {
        return new CurationCaseSnapshot(
                id, CurationOriginType.TICKET, "ticket-9", status,
                "Recorrência", "pedro", now(), now());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.parse("2026-07-14T18:00:00-03:00");
    }
}
