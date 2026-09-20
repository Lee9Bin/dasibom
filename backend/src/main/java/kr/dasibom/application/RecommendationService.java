package kr.dasibom.application;

import com.fasterxml.jackson.databind.JsonNode;
import kr.dasibom.domain.Region;
import kr.dasibom.infrastructure.TourApiClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class RecommendationService {
    private static final Map<String,String> TYPES=Map.of("ATTRACTION","12","FOOD","39","STAY","32");
    private final TourApiClient api;
    public RecommendationService(TourApiClient api){this.api=api;}

    public Map<String,Object> recommendations(Region region){
        Map<String,Object> categories=new LinkedHashMap<>();
        for(var entry:TYPES.entrySet()){
            var page=api.fetchPage("KorService2/areaBasedList2",Map.of("lDongRegnCd",region.getAreaCode(),"lDongSignguCd",region.getCode().substring(2),"arrange","Q","contentTypeId",entry.getValue()),50,1);
            categories.put(entry.getKey(),Map.of("items",page.items().stream().map(this::place).toList(),"available",page.totalCount(),"shown",page.items().size()));
        }
        return Map.of("categories",categories,"fetchedAt",Instant.now(),"mode","LIVE","source","출처: ⓒ한국관광공사");
    }
    private Map<String,Object> place(JsonNode n){
        Map<String,Object> p=new LinkedHashMap<>();
        for(String key:List.of("contentid","title","addr1","addr2","firstimage","firstimage2","cpyrhtDivCd","mapx","mapy","tel"))p.put(key,n.path(key).asText(""));
        p.put("firstimage",Optional.ofNullable(CatalogService.safeImage(n.path("firstimage").asText())).orElse(""));
        return p;
    }
}
