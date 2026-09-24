package kr.dasibom.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
public class TourApiClient {
    public record PagedResult(List<JsonNode> items,int totalCount,int pageNo,int numOfRows) {}
    private static final Logger log=LoggerFactory.getLogger(TourApiClient.class);
    private final RestClient client;
    private final HttpClient imageClient;
    private final ObjectMapper mapper;
    private final String key;
    public TourApiClient(ObjectMapper mapper, @Value("${dasibom.tour-api-key}") String key) {
        this.mapper = mapper;
        this.key = key;
        this.imageClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
        var factory = new JdkClientHttpRequestFactory(imageClient);
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.client = RestClient.builder().requestFactory(factory).build();
    }
    public boolean configured() { return !key.isBlank(); }
    public List<JsonNode> fetch(String operation, Map<String,String> params, int rows) {
        return fetchPage(operation,params,rows,1).items();
    }
    public PagedResult fetchPage(String operation, Map<String,String> params, int rows,int page) {
        if (!configured()) throw new TourApiException("API_KEY_MISSING");
        long started=System.nanoTime();
        var uri = UriComponentsBuilder.fromUriString("https://apis.data.go.kr/B551011/" + operation)
            .queryParam("serviceKey", key).queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "DasibomKorea").queryParam("_type", "json")
            .queryParam("numOfRows", rows).queryParam("pageNo", page);
        params.forEach(uri::queryParam);
        try {
            String raw = client.get().uri(uri.build().encode().toUri()).retrieve().body(String.class);
            var result=parsePage(mapper.readTree(raw));
            log.info("tourapi_call operation={} status=success items={} page={} total={} durationMs={}",operation,result.items().size(),page,result.totalCount(),elapsedMillis(started));
            return result;
        } catch (TourApiException e) { log.warn("tourapi_call operation={} status={} durationMs={}",operation,e.getMessage(),elapsedMillis(started));throw e; }
        catch (Exception e) {
            // Never propagate the original exception: its URL can contain the service key.
            log.warn("tourapi_call operation={} status=unavailable durationMs={}",operation,elapsedMillis(started));
            throw new TourApiException("UPSTREAM_UNAVAILABLE");
        }
    }
    private static long elapsedMillis(long started){return (System.nanoTime()-started)/1_000_000;}
    public boolean imageAvailable(String rawUrl) {
        try {
            URI uri=URI.create(rawUrl.replaceFirst("^http:","https:"));
            if(!"https".equalsIgnoreCase(uri.getScheme()) || !"tong.visitkorea.or.kr".equalsIgnoreCase(uri.getHost()))return false;
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3))
                .header("Range","bytes=0-0").header("User-Agent","DasibomKorea/1.0").GET().build();
            var response=imageClient.send(request,HttpResponse.BodyHandlers.discarding());
            return usableImageResponse(response.statusCode(),response.headers().firstValue("Content-Type"));
        }catch(Exception e){
            if(e instanceof InterruptedException)Thread.currentThread().interrupt();
            return false;
        }
    }
    public static boolean usableImageResponse(int status,Optional<String> contentType) {
        return (status==200 || status==206) && contentType.map(v->v.toLowerCase(Locale.ROOT).startsWith("image/")).orElse(false);
    }
    public static List<JsonNode> parse(JsonNode root) {
        return parsePage(root).items();
    }
    public static PagedResult parsePage(JsonNode root) {
        JsonNode response = root.path("response");
        String code = response.path("header").path("resultCode").asText();
        if (!"0000".equals(code) && !"00".equals(code)) throw new TourApiException("UPSTREAM_REJECTED");
        JsonNode body=response.path("body");
        JsonNode items = body.path("items").path("item");
        List<JsonNode> result=new ArrayList<>();
        if (!(items.isMissingNode() || items.isNull() || items.isTextual())) {
            if (items.isObject()) result.add(items);
            else if (items.isArray()) items.forEach(result::add);
            else throw new TourApiException("UPSTREAM_FORMAT_ERROR");
        }
        return new PagedResult(List.copyOf(result),body.path("totalCount").asInt(result.size()),body.path("pageNo").asInt(1),body.path("numOfRows").asInt(result.size()));
    }
    public static class TourApiException extends RuntimeException {
        public TourApiException(String code) { super(code); }
    }
}
