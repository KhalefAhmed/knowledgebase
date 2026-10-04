package example;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Post;

import java.util.Map;

@Controller("/api")
class KnowledgeController {
    private final KnowledgeService knowledgeService;

    KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Post("/documents")
    HttpResponse<?> ingest(@Body IngestRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("request body must not be null");
            }
            int chunks = knowledgeService.ingest(request.source(), request.content());
            return HttpResponse.created(new IngestResponse(request.source(), chunks));
        } catch (IllegalArgumentException exception) {
            return HttpResponse.badRequest(Map.of("message", exception.getMessage()));
        }
    }

    @Delete("/documents/{source}")
    HttpResponse<?> delete(String source) {
        try {
            knowledgeService.delete(source);
            return HttpResponse.noContent();
        } catch (IllegalArgumentException exception) {
            return HttpResponse.badRequest(Map.of("message", exception.getMessage()));
        }
    }

    @Post("/ask")
    HttpResponse<?> ask(@Body AskRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("request body must not be null");
            }
            return HttpResponse.ok(knowledgeService.ask(request.question(), request.maxResults()));
        } catch (IllegalArgumentException exception) {
            return HttpResponse.badRequest(Map.of("message", exception.getMessage()));
        }
    }

}
