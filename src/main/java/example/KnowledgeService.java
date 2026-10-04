package example;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
class KnowledgeService {
    private static final int MAX_CONTENT_LENGTH = 500_000;
    private static final int MAX_QUESTION_LENGTH = 4_000;
    private static final int MAX_RESULTS = 10;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;

    KnowledgeService(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            ChatModel chatModel
    ) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
    }

    int ingest(String source, String content) {
        requireText(source, "source");
        requireText(content, "content");
        if (source.length() > 200) {
            throw new IllegalArgumentException("source must be at most 200 characters");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content must be at most 500000 characters");
        }

        List<String> chunks = TextChunker.split(content);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("content must contain text");
        }

        List<TextSegment> segments = chunks.stream()
                .map(chunk -> TextSegment.from(chunk, new Metadata().put("source", source)))
                .toList();
        var embeddings = embeddingModel.embedAll(segments).content();

        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("source").isEqualTo(source));
        embeddingStore.addAll(embeddings, segments);
        return segments.size();
    }

    void delete(String source) {
        requireText(source, "source");
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("source").isEqualTo(source));
    }

    AskResponse ask(String question, Integer requestedMaxResults) {
        requireText(question, "question");
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new IllegalArgumentException("question must be at most 4000 characters");
        }

        int maxResults = requestedMaxResults == null ? 5 : requestedMaxResults;
        if (maxResults < 1 || maxResults > MAX_RESULTS) {
            throw new IllegalArgumentException("maxResults must be between 1 and 10");
        }

        var queryEmbedding = embeddingModel.embed(question).content();
        var searchResult = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .build());

        List<KnowledgeSource> sources = searchResult.matches().stream()
                .map(match -> new KnowledgeSource(
                        match.embedded().metadata().getString("source"),
                        match.embedded().text(),
                        match.score() == null ? 0.0 : match.score()
                ))
                .toList();

        if (sources.isEmpty()) {
            return new AskResponse(
                    "Je ne trouve pas d'information pertinente dans la base de connaissances.",
                    sources
            );
        }

        String context = sources.stream()
                .map(source -> "[Source: " + source.source() + "]\n" + source.text())
                .reduce((left, right) -> left + "\n\n---\n\n" + right)
                .orElseThrow();
        String prompt = """
                Réponds dans la même langue que la question, uniquement à partir du contexte fourni.
                Si le contexte ne permet pas de répondre, dis-le clairement. N'invente aucun fait.
                Cite les sources utilisées sous la forme [nom de source].

                Contexte:
                %s

                Question:
                %s
                """.formatted(context, question);

        return new AskResponse(chatModel.chat(prompt), sources);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
