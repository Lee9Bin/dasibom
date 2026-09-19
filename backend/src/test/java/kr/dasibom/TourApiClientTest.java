package kr.dasibom;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.dasibom.infrastructure.TourApiClient;
import kr.dasibom.application.CatalogService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TourApiClientTest {
    private final ObjectMapper mapper=new ObjectMapper();
    @Test void normalizesSingleAndArrayItems() throws Exception {
        for(String item:java.util.List.of("{\"id\":1}","[{\"id\":1}]")) {
            var root=mapper.readTree("{\"response\":{\"header\":{\"resultCode\":\"0000\"},\"body\":{\"items\":{\"item\":"+item+"}}}}");
            assertThat(TourApiClient.parse(root)).hasSize(1);
        }
    }
    @Test void handlesEmptyItems()throws Exception{assertThat(TourApiClient.parse(mapper.readTree("{\"response\":{\"header\":{\"resultCode\":\"0000\"},\"body\":{\"items\":\"\"}}}"))).isEmpty();}
    @Test void rejectsBusinessErrorsEvenWithHttp200()throws Exception{var json=mapper.readTree("{\"response\":{\"header\":{\"resultCode\":\"10\"}}}");assertThatThrownBy(()->TourApiClient.parse(json)).hasMessage("UPSTREAM_REJECTED");}
    @Test void permitsOnlyTourismImageHost(){assertThat(CatalogService.safeImage("http://tong.visitkorea.or.kr/a.jpg")).isEqualTo("https://tong.visitkorea.or.kr/a.jpg");assertThat(CatalogService.safeImage("https://tong.visitkorea.or.kr.evil.test/a")).isNull();assertThat(CatalogService.safeImage("javascript:alert(1)")).isNull();}
    @Test void acceptsOnlySuccessfulImageResponses(){
        assertThat(TourApiClient.usableImageResponse(206,java.util.Optional.of("image/jpg"))).isTrue();
        assertThat(TourApiClient.usableImageResponse(200,java.util.Optional.of("image/jpeg; charset=binary"))).isTrue();
        assertThat(TourApiClient.usableImageResponse(404,java.util.Optional.of("text/html"))).isFalse();
        assertThat(TourApiClient.usableImageResponse(200,java.util.Optional.of("text/html"))).isFalse();
        assertThat(TourApiClient.usableImageResponse(206,java.util.Optional.empty())).isFalse();
    }
}
