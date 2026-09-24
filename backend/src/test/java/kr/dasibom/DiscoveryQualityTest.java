package kr.dasibom;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.dasibom.application.*;
import kr.dasibom.domain.*;
import kr.dasibom.infrastructure.TourApiClient;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscoveryQualityTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private Map<String,Object> photo(String id,String title,String venue,String type){
        return Map.of("id",id,"title",title,"placeName",venue,"photoType",type,"url","https://tong.visitkorea.or.kr/"+id+".jpg","location","구례군 "+venue);
    }
    @Test void keepsOneRepresentativePerPlaceAcrossAwardsAndDifferentTitles(){
        var selected=PhotoSelection.diverse(List.of(photo("a","화엄사 홍매화","화엄사 홍매화","AWARD"),photo("b","화엄사의 봄","화엄사","GALLERY"),photo("c","화엄사 가을","화엄사","GALLERY"),photo("d","사성암 풍경","사성암","GALLERY"),photo("e","노고단의 아침","지리산 국립공원","GALLERY")));
        assertEquals(3,selected.size());assertEquals("화엄사",selected.getFirst().get("placeName"));
        assertEquals(3,selected.stream().map(p->p.get("placeKey")).distinct().count());
        var creative=new HashMap<>(photo("f","산수유 꽃피는 봄","산수유 꽃피는 봄","GALLERY"));
        creative.put("location","구례군 산동면, 산수유마을");
        var village=new HashMap<>(photo("g","지리산 아래 산수유마을","구례산수유마을","GALLERY"));
        village.put("regionName","구례군");
        var resolved=PhotoSelection.diverse(List.of(village,creative));
        assertEquals(1,resolved.size());assertEquals("산수유마을",resolved.getFirst().get("placeKey"));
    }
    @Test void parsesOfficialIxFieldsAndDoesNotInventValues() throws Exception {
        var result=MetricService.parseMetrics(List.of(mapper.readTree("{\"tarSvcDemIxCd\":\"1101\",\"tarSvcDemIxNm\":\"레포츠 SNS 언급량\",\"tarSvcDemIxVal\":\"42.3\"}"),mapper.readTree("{\"tarSvcDemIxCd\":\"1102\",\"tarSvcDemIxVal\":\"NaN\"}")),false);
        assertEquals(1,result.size());assertEquals("ACTIVITY",result.getFirst().get("theme"));assertEquals(42.3,result.getFirst().get("value"));
        var nature=MetricService.parseMetrics(List.of(mapper.readTree("{\"culResDemIxCd\":\"1205\",\"culResDemIxVal\":\"68.8\"}")),true);
        assertEquals("NATURE",nature.getFirst().get("theme"));
    }
    @Test void mapsOnlyOneToOneAdministrativeAliases(){
        assertEquals("12730",RegionCodes.currentCode("46730"));assertEquals("46730",RegionCodes.serviceCode("12730"));
        assertEquals("28125",RegionCodes.serviceCode("28125")); // Merged districts cannot be apportioned without evidence.
    }
    @Test void latestVisitorsConnectsRenamedCodesAndGivesEqualCountsEqualScores() throws Exception {
        var api=mock(TourApiClient.class);
        when(api.fetch(anyString(),anyMap(),eq(1000))).thenReturn(List.of(mapper.readTree("{\"signguCode\":\"12730\",\"touNum\":\"100\"}"),mapper.readTree("{\"signguCode\":\"11110\",\"touNum\":\"100\"}"),mapper.readTree("{\"signguCode\":\"11140\",\"touNum\":\"200\"}")));
        var result=new VisitorInsightService(api).latest();assertEquals(100L,result.visitors().get("46730"));assertEquals(result.hiddenScores().get("46730"),result.hiddenScores().get("11110"));
    }
    @Test void galleryPaginatesTheCuratedPlacesWithoutSkippingValidRepresentatives() throws Exception {
        var region=mock(Region.class);when(region.getCode()).thenReturn("46730");when(region.getName()).thenReturn("구례군");when(region.getAreaCode()).thenReturn("46");
        var regions=mock(RegionRepository.class);when(regions.findById("46730")).thenReturn(Optional.of(region));var api=mock(TourApiClient.class);
        List<com.fasterxml.jackson.databind.JsonNode> photos=new ArrayList<>();
        for(int n=0;n<30;n++)photos.add(mapper.readTree("{\"galContentId\":\""+n+"\",\"galTitle\":\"장소"+(n/2)+"\",\"galSearchKeyword\":\"장소"+(n/2)+", 구례군\",\"galPhotographyLocation\":\"전라남도 구례군\",\"galWebImageUrl\":\"https://tong.visitkorea.or.kr/"+n+".jpg\"}"));
        when(api.fetchPage(anyString(),anyMap(),eq(100),eq(1))).thenReturn(new TourApiClient.PagedResult(photos,30,1,100));when(api.fetch(anyString(),anyMap(),eq(100))).thenReturn(List.of());when(api.imageAvailable(anyString())).thenReturn(true);
        var service=new CatalogService(regions,mock(LikeRepository.class),api,mock(VisitorInsightService.class));var first=service.photoPage("46730",1,12);var second=service.photoPage("46730",2,12);
        assertEquals(12,first.items().size());assertEquals(3,second.items().size());assertTrue(first.hasMore());assertFalse(second.hasMore());assertEquals(15,first.total());
        Set<Object> ids=new HashSet<>();first.items().forEach(p->ids.add(p.get("id")));second.items().forEach(p->assertTrue(ids.add(p.get("id"))));
    }
}
