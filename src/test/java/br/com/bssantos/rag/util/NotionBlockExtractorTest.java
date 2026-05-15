package br.com.bssantos.rag.util;

import br.com.bssantos.rag.dto.notion.NotionBlockResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotionBlockExtractorTest {

    private NotionBlockExtractor extractor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        extractor = new NotionBlockExtractor();
        objectMapper = new ObjectMapper();
    }

    // --- Helper para construir NotionBlockResponse via Jackson ---

    /**
     * Monta um NotionBlockResponse desserializando um JSON construído programaticamente.
     * O JSON tem a estrutura: { "type": "<tipo>", "<tipo>": { "rich_text": [...] } }
     * Isso aciona o @JsonAnySetter e preenche o mapa content corretamente.
     */
    private NotionBlockResponse criarBloco(String tipo, String... textos) throws Exception {
        ObjectNode raiz = objectMapper.createObjectNode();
        raiz.put("type", tipo);

        ObjectNode conteudoTipo = objectMapper.createObjectNode();
        ArrayNode richText = objectMapper.createArrayNode();
        for (String texto : textos) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("plain_text", texto);
            richText.add(node);
        }
        conteudoTipo.set("rich_text", richText);
        raiz.set(tipo, conteudoTipo);

        return objectMapper.treeToValue(raiz, NotionBlockResponse.class);
    }

    // -------------------------------------------------------------------------
    // Casos 1-7: tipos suportados com prefixo correto
    // -------------------------------------------------------------------------

    @Test
    void heading1_deveRetornarTextoComPrefixoCerquilha() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("heading_1", "Introdução");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("# Introdução", resultado);
    }

    @Test
    void heading2_deveRetornarTextoComDuplasCerquilhas() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("heading_2", "Seção Principal");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("## Seção Principal", resultado);
    }

    @Test
    void heading3_deveRetornarTextoComTriplasCerquilhas() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("heading_3", "Subseção");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("### Subseção", resultado);
    }

    @Test
    void paragraph_deveRetornarTextoSemPrefixo() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("paragraph", "Texto do parágrafo.");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("Texto do parágrafo.", resultado);
    }

    @Test
    void bulletedListItem_deveRetornarTextoSemPrefixo() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("bulleted_list_item", "Item da lista");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("Item da lista", resultado);
    }

    @Test
    void quote_deveRetornarTextoSemPrefixo() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("quote", "Uma citação importante.");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("Uma citação importante.", resultado);
    }

    @Test
    void code_deveRetornarTextoSemPrefixo() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("code", "System.out.println(\"Olá\");");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("System.out.println(\"Olá\");", resultado);
    }

    // -------------------------------------------------------------------------
    // Caso 8: tipo desconhecido deve ser ignorado
    // -------------------------------------------------------------------------

    @Test
    void tipoDesconhecido_deveSerIgnoradoNoResultado() throws Exception {
        // Arrange
        NotionBlockResponse tipoDesconhecido = criarBloco("divider", "conteudo irrelevante");
        NotionBlockResponse paragrafo = criarBloco("paragraph", "Texto válido.");

        // Act
        String resultado = extractor.extract(List.of(tipoDesconhecido, paragrafo));

        // Assert
        assertEquals("Texto válido.", resultado);
    }

    @Test
    void listaApenasComTiposDesconhecidos_deveRetornarStringVazia() throws Exception {
        // Arrange
        NotionBlockResponse divider = criarBloco("divider", "nada");
        NotionBlockResponse table = criarBloco("table", "nada");

        // Act
        String resultado = extractor.extract(List.of(divider, table));

        // Assert
        assertEquals("", resultado);
    }

    // -------------------------------------------------------------------------
    // Caso 9: bloco com texto em branco deve ser ignorado
    // -------------------------------------------------------------------------

    @Test
    void blocoComTextoEmBranco_deveSerIgnorado() throws Exception {
        // Arrange
        NotionBlockResponse blocoVazio = criarBloco("paragraph", "   ");
        NotionBlockResponse blocoValido = criarBloco("paragraph", "Conteúdo real.");

        // Act
        String resultado = extractor.extract(List.of(blocoVazio, blocoValido));

        // Assert
        assertEquals("Conteúdo real.", resultado);
    }

    @Test
    void blocoComRichTextVazio_deveSerIgnorado() throws Exception {
        // Arrange — rich_text sem nenhum node produz texto vazio ""
        NotionBlockResponse blocoSemTexto = criarBloco("paragraph"); // sem textos
        NotionBlockResponse blocoValido = criarBloco("paragraph", "Texto presente.");

        // Act
        String resultado = extractor.extract(List.of(blocoSemTexto, blocoValido));

        // Assert
        assertEquals("Texto presente.", resultado);
    }

    // -------------------------------------------------------------------------
    // Caso 10: heading entre parágrafos gera \n antes do prefixo
    // -------------------------------------------------------------------------

    @Test
    void headingEntreParafagos_deveGerarLinhaEmBrancoAntesDoHeading() throws Exception {
        // Arrange
        NotionBlockResponse paragrafo1 = criarBloco("paragraph", "Primeiro parágrafo.");
        NotionBlockResponse heading = criarBloco("heading_2", "Título do Meio");
        NotionBlockResponse paragrafo2 = criarBloco("paragraph", "Segundo parágrafo.");

        // Act
        String resultado = extractor.extract(List.of(paragrafo1, heading, paragrafo2));

        // Assert
        // joining("\n") entre cada bloco formatado:
        //   "Primeiro parágrafo." + "\n" + "\n## Título do Meio" + "\n" + "Segundo parágrafo."
        // = "Primeiro parágrafo.\n\n## Título do Meio\nSegundo parágrafo."
        assertEquals("Primeiro parágrafo.\n\n## Título do Meio\nSegundo parágrafo.", resultado);
    }

    // -------------------------------------------------------------------------
    // Caso 11: primeiro bloco sendo heading — sem \n líder após .strip()
    // -------------------------------------------------------------------------

    @Test
    void primeiroBloco_sendoHeading_naoDeveTeNewlineLider() throws Exception {
        // Arrange
        NotionBlockResponse heading = criarBloco("heading_1", "Capítulo Um");
        NotionBlockResponse paragrafo = criarBloco("paragraph", "Conteúdo do capítulo.");

        // Act
        String resultado = extractor.extract(List.of(heading, paragrafo));

        // Assert
        assertFalse(resultado.startsWith("\n"), "Resultado não deve começar com newline");
        assertEquals("# Capítulo Um\nConteúdo do capítulo.", resultado);
    }

    @Test
    void ultimoBloco_sendoHeading_naoDeveTeNewlineNoFinal() throws Exception {
        // Arrange
        NotionBlockResponse paragrafo = criarBloco("paragraph", "Introdução.");
        NotionBlockResponse heading = criarBloco("heading_1", "Fim");

        // Act
        String resultado = extractor.extract(List.of(paragrafo, heading));

        // Assert
        assertFalse(resultado.endsWith("\n"), "Resultado não deve terminar com newline");
        assertEquals("Introdução.\n\n# Fim", resultado);
    }

    // -------------------------------------------------------------------------
    // Caso 12: rich_text com múltiplos nodes — concatena todos os plain_text
    // -------------------------------------------------------------------------

    @Test
    void richTextComMultiplosNodes_deveConcatenarTodosOsTextos() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("paragraph", "Texto ", "em ", "negrito", ".");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("Texto em negrito.", resultado);
    }

    @Test
    void heading1ComMultiplosNodes_deveConcatenarComPrefixo() throws Exception {
        // Arrange
        NotionBlockResponse bloco = criarBloco("heading_1", "Parte ", "Um");

        // Act
        String resultado = extractor.extract(List.of(bloco));

        // Assert
        assertEquals("# Parte Um", resultado);
    }

    // -------------------------------------------------------------------------
    // Cenário integrado: múltiplos tipos na ordem real de um documento
    // -------------------------------------------------------------------------

    @Test
    void documentoCompleto_deveFormatarTodosOsTiposCorretamente() throws Exception {
        // Arrange
        List<NotionBlockResponse> blocos = List.of(
                criarBloco("heading_1", "Capítulo 1"),
                criarBloco("paragraph", "Descrição do capítulo."),
                criarBloco("heading_2", "Seção 1.1"),
                criarBloco("bulleted_list_item", "Item A"),
                criarBloco("bulleted_list_item", "Item B"),
                criarBloco("quote", "Citação relevante."),
                criarBloco("code", "int x = 42;"),
                criarBloco("divider")  // deve ser ignorado
        );

        // Act
        String resultado = extractor.extract(blocos);

        // Assert
        String esperado = "# Capítulo 1\n" +
                "Descrição do capítulo.\n" +
                "\n## Seção 1.1\n" +
                "Item A\n" +
                "Item B\n" +
                "Citação relevante.\n" +
                "int x = 42;";
        assertEquals(esperado, resultado);
    }

    // -------------------------------------------------------------------------
    // Lista vazia
    // -------------------------------------------------------------------------

    @Test
    void listaVazia_deveRetornarStringVazia() {
        // Arrange + Act
        String resultado = extractor.extract(List.of());

        // Assert
        assertEquals("", resultado);
    }
}
