package br.com.infocedro.clipper.ticket;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.infocedro.clipper.clipper.ClipperAgent;
import br.com.infocedro.clipper.clipper.DiagnosisFeedback;
import br.com.infocedro.clipper.clipper.DiagnosisNotFoundException;
import br.com.infocedro.clipper.curation.CurationCaseCreationResult;
import br.com.infocedro.clipper.curation.CurationCaseService;
import br.com.infocedro.clipper.curation.CurationCaseSnapshot;
import br.com.infocedro.clipper.curation.CurationOriginType;
import br.com.infocedro.clipper.curation.CurationStatus;

// Testa a borda HTTP das ações B3. Os testes de serviço travam as regras;
// estes travam o que o cliente realmente recebe depois que o Spring MVC
// resolve rota, body e @ResponseStatus.
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private ClipperAgent clipperAgent;

    @MockitoBean
    private CurationCaseService curationCaseService;

    @Test
    void respostaVaziaRetorna400() throws Exception {
        when(ticketService.reply(7L, "   "))
                .thenThrow(new InvalidTicketActionException("Resposta não pode ser vazia."));

        mockMvc.perform(post("/api/tickets/7/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"response\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void feedbackDeTicketInexistenteRetorna404() throws Exception {
        when(ticketService.findById(99L)).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(post("/api/tickets/99/diagnosis/feedback"))
                .andExpect(status().isNotFound());
    }

    @Test
    void feedbackSemDiagnosticoRetorna409() throws Exception {
        when(ticketService.findById(7L)).thenReturn(new Ticket());
        when(clipperAgent.flagIncorrect(7L, null)).thenThrow(new DiagnosisNotFoundException(7L));

        mockMvc.perform(post("/api/tickets/7/diagnosis/feedback"))
                .andExpect(status().isConflict());
    }

    @Test
    void feedbackSemBodyRetorna201ComRecibo() throws Exception {
        Instant createdAt = Instant.parse("2026-07-10T18:00:00Z");
        DiagnosisFeedback feedback = mock(DiagnosisFeedback.class);
        when(ticketService.findById(7L)).thenReturn(new Ticket());
        when(clipperAgent.flagIncorrect(7L, null)).thenReturn(feedback);
        when(feedback.getId()).thenReturn(42L);
        when(feedback.getCreatedAt()).thenReturn(createdAt);

        mockMvc.perform(post("/api/tickets/7/diagnosis/feedback"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedbackId").value(42))
                .andExpect(jsonPath("$.createdAt").value("2026-07-10T18:00:00Z"));

        verify(clipperAgent).flagIncorrect(7L, null);
    }

    @Test
    void enviaTicketParaCuradoriaEDevolve201NaPrimeiraCriacao() throws Exception {
        when(ticketService.findById(7L)).thenReturn(new Ticket());
        CurationCaseSnapshot snapshot = new CurationCaseSnapshot(
                22L, CurationOriginType.TICKET, "7", CurationStatus.ABERTO,
                "Falha recorrente", "pedro", null, null);
        when(curationCaseService.createFromTicket(7L, "pedro", "Falha recorrente"))
                .thenReturn(new CurationCaseCreationResult(snapshot, true));

        mockMvc.perform(post("/api/tickets/7/curation-case")
                        .header("X-Actor", "pedro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Falha recorrente\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.curationCase.id").value(22));
    }

    @Test
    void reutilizaCasoExistenteSemDuplicar() throws Exception {
        when(ticketService.findById(7L)).thenReturn(new Ticket());
        CurationCaseSnapshot snapshot = new CurationCaseSnapshot(
                22L, CurationOriginType.TICKET, "7", CurationStatus.ABERTO,
                "Falha recorrente", "pedro", null, null);
        when(curationCaseService.createFromTicket(7L, "pedro", "Reabrir curadoria"))
                .thenReturn(new CurationCaseCreationResult(snapshot, false));

        mockMvc.perform(post("/api/tickets/7/curation-case")
                        .header("X-Actor", "pedro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Reabrir curadoria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false));
    }
}
