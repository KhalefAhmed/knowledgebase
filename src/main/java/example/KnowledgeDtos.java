package example;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
record IngestRequest(String source, String content) {
}

@Serdeable
record IngestResponse(String source, int chunksIndexed) {
}

@Serdeable
record AskRequest(String question, Integer maxResults) {
}

@Serdeable
record KnowledgeSource(String source, String text, double score) {
}

@Serdeable
record AskResponse(String answer, List<KnowledgeSource> sources) {
}
