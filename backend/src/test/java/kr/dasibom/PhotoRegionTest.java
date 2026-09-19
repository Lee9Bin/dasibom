package kr.dasibom;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.dasibom.application.CatalogService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhotoRegionTest {
    private final ObjectMapper mapper=new ObjectMapper();
    @Test void rejectsSameWordInAnUnrelatedPlace() throws Exception {
        assertFalse(CatalogService.matchesPhotoRegion("영월군",mapper.readTree("{\"galPhotographyLocation\":\"전라남도 영암\",\"galSearchKeyword\":\"왕인박사유적지 영월관\"}")));
        assertFalse(CatalogService.matchesPhotoRegion("남해군",mapper.readTree("{\"galPhotographyLocation\":\"경상남도 거제시\",\"galSearchKeyword\":\"남해바다, 거제관광\"}")));
    }
    @Test void acceptsAdministrativeAndShortLocationNames() throws Exception {
        assertTrue(CatalogService.matchesPhotoRegion("남해군",mapper.readTree("{\"galPhotographyLocation\":\"경상남도 남해\"}")));
        assertTrue(CatalogService.matchesPhotoRegion("영월군",mapper.readTree("{\"galPhotographyLocation\":\"강원특별자치도 영월군 영월읍\"}")));
        assertTrue(CatalogService.matchesPhotoRegion("원주시",mapper.readTree("{\"galPhotographyLocation\":\"강원도 치악산국립공원\",\"galSearchKeyword\":\"치악산, 강원특별자치도 원주시, 가을\"}")));
    }
}
