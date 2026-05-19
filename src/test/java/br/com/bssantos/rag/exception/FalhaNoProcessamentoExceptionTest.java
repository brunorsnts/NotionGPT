package br.com.bssantos.rag.exception;

import br.com.bssantos.rag.observability.FailureStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FalhaNoProcessamentoExceptionTest {

    @Test
    void construtorComMensagem_defineFailureStageComo_UNKNOWN() {
        // Arrange & Act
        FalhaNoProcessamentoException exception = new FalhaNoProcessamentoException("msg");

        // Assert
        assertThat(exception.getFailureStage()).isEqualTo(FailureStage.UNKNOWN);
    }

    @Test
    void construtorComMensagemECausa_defineFailureStageComo_UNKNOWN_ePreservaCausa() {
        // Arrange
        RuntimeException causa = new RuntimeException("causa");

        // Act
        FalhaNoProcessamentoException exception = new FalhaNoProcessamentoException("msg", causa);

        // Assert
        assertThat(exception.getFailureStage()).isEqualTo(FailureStage.UNKNOWN);
        assertThat(exception.getCause().getMessage()).isEqualTo("causa");
    }
}
