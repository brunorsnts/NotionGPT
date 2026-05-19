package br.com.bssantos.rag.dto;

import java.util.List;

public record RetrievalResult(
        String context,
        int matchesCount,
        List<Double> scores,
        List<String> titulos
) {
}
