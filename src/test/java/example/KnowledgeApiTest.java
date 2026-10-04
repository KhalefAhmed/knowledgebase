package example;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste l'API avec un vrai Infinispan démarré par le module Test Resources.
 * Les modèles Ollama sont remplacés par des faux pour ne pas dépendre d'Ollama.
 */
@MicronautTest(transactional = false)
class KnowledgeApiTest {

    private static final int DIMENSION = 768;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void ingestThenAskReturnsTheIngestedSource() {
        var created = client.toBlocking().exchange(
                HttpRequest.POST("/api/documents", Map.of(
                        "source", "guide.md",
                        "content", "Infinispan supporte la recherche vectorielle.")),
                Map.class);
        assertEquals(HttpStatus.CREATED, created.getStatus());
        assertEquals(1, created.body().get("chunksIndexed"));

        var answer = client.toBlocking().retrieve(
                HttpRequest.POST("/api/ask", Map.of("question", "Infinispan supporte la recherche vectorielle.")),
                Map.class);
        assertEquals("réponse de test", answer.get("answer"));
        var sources = (List<?>) answer.get("sources");
        assertEquals("guide.md", ((Map<?, ?>) sources.getFirst()).get("source"));
    }

    @Test
    void deleteRemovesTheSource() {
        client.toBlocking().exchange(HttpRequest.POST("/api/documents", Map.of(
                "source", "temp.md", "content", "Contenu temporaire à supprimer.")));
        var deleted = client.toBlocking().exchange(HttpRequest.DELETE("/api/documents/temp.md"));
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatus());

        var answer = client.toBlocking().retrieve(
                HttpRequest.POST("/api/ask", Map.of("question", "Contenu temporaire à supprimer.")),
                Map.class);
        var sources = (List<?>) answer.get("sources");
        assertTrue(sources.stream().noneMatch(s -> "temp.md".equals(((Map<?, ?>) s).get("source"))));
    }

    @Factory
    static class FakeModels {

        @Singleton
        @Primary
        EmbeddingModel embeddingModel() {
            return new EmbeddingModel() {
                @Override
                public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
                    return Response.from(segments.stream().map(s -> vector(s.text())).toList());
                }
            };
        }

        @Singleton
        @Primary
        ChatModel chatModel() {
            return new ChatModel() {
                @Override
                public String chat(String message) {
                    return "réponse de test";
                }

                @Override
                public dev.langchain4j.model.chat.response.ChatResponse doChat(
                        dev.langchain4j.model.chat.request.ChatRequest request) {
                    return dev.langchain4j.model.chat.response.ChatResponse.builder()
                            .aiMessage(dev.langchain4j.data.message.AiMessage.from("réponse de test"))
                            .build();
                }
            };
        }

        // Vecteur déterministe : même texte, même vecteur (score maximal).
        private static Embedding vector(String text) {
            float[] values = new float[DIMENSION];
            for (int i = 0; i < DIMENSION; i++) {
                values[i] = 1f + ((text.hashCode() >>> (i % 16)) & 7);
            }
            return Embedding.from(values);
        }
    }
}
