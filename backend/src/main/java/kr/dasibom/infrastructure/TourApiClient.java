package kr.dasibom.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Component
public class TourApiClient {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String key;
    public TourApiClient(ObjectMapper mapper, @Value("${dasibom.tour-api-key}") String key) {
        this.mapper = mapper;
        this.key = key;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.client = RestClient.builder().requestFactory(factory).build();
    }
    public boolean configured() { return !key.isBlank(); }
    public List<JsonNode> fetch(String operation, Map<String,String> params, int rows) {
        if (!configured()) throw new TourApiException("API_KEY_MISSING");
        var uri = UriComponentsBuilder.fromUriString("https://apis.data.go.kr/B551011/" + operation)
            .queryParam("serviceKey", key).queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "DasibomKorea").queryParam("_type", "json")
            .queryParam("numOfRows", rows).queryParam("pageNo", 1);
        params.forEach(uri::queryParam);
        try {
            String raw = client.get().uri(uri.build().encode().toUri()).retrieve().body(String.class);
            return parse(mapper.readTree(raw));
        } catch (TourApiException e) { throw e; }
        catch (Exception e) {
            // Never propagate the original exception: its URL can contain the service key.
            throw new TourApiException("UPSTREAM_UNAVAILABLE");
        }
    }
    public static List<JsonNode> parse(JsonNode root) {
        JsonNode response = root.path("response");
        String code = response.path("header").path("resultCode").asText();
        if (!"0000".equals(code) && !"00".equals(code)) throw new TourApiException("UPSTREAM_REJECTED");
        JsonNode items = response.path("body").path("items").path("item");
        if (items.isMissingNode() || items.isNull() || items.isTextual()) return List.of();
        if (items.isObject()) return List.of(items);
        if (items.isArray()) { List<JsonNode> result = new ArrayList<>(); items.forEach(result::add); return result; }
        throw new TourApiException("UPSTREAM_FORMAT_ERROR");
    }
    public static class TourApiException extends RuntimeException {
        public TourApiException(String code) { super(code); }
    }
}

