package kr.dasibom.application;

import com.fasterxml.jackson.databind.JsonNode;
import kr.dasibom.domain.*;
import kr.dasibom.infrastructure.DatasetStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class CatalogService {
    private final RegionRepository regions;
    private final DatasetStore store;
    private final LikeRepository likes;
    public CatalogService(RegionRepository regions, DatasetStore store, LikeRepository likes) { this.regions=regions;this.store=store;this.likes=likes; }
    public Region find(String code) { return regions.findById(code).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"지역을 찾을 수 없습니다.")); }
    public List<Region> all() { return regions.findAll().stream().sorted(Comparator.comparing(Region::getCode)).toList(); }
    public Map<String,Object> summary(Region r) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("code",r.getCode());result.put("name",r.getName());result.put("areaCode",r.getAreaCode());result.put("areaName",r.getAreaName());
        result.put("tagline",r.getTagline());result.put("theme",r.getTheme());result.put("latitude",r.getLatitude());result.put("longitude",r.getLongitude());
        result.put("anchorPlace",r.getAnchorPlace());result.put("source","한국관광공사 TourAPI");
        var photos=photos(r.getCode()); result.put("heroPhoto",photos.isEmpty()?null:photos.getFirst());
        result.put("dataStatus",photos.isEmpty()?"PENDING":"AVAILABLE");result.put("hiddenScore",null);result.put("attractionScore",null);
        result.put("likes",likes.countByIdRegionCode(r.getCode()));
        return result;
    }
    public List<Map<String,Object>> photos(String code) {
        String name=find(code).getName();
        List<Map<String,Object>> photos=new ArrayList<>(); Set<String> urls=new HashSet<>();
        for(JsonNode n:store.read(code,"photos")) {
            if(!matchesPhotoRegion(name,n))continue;
            String url=safeImage(n.path("galWebImageUrl").asText());
            if(url==null || !urls.add(url))continue;
            photos.add(Map.of("id",n.path("galContentId").asText(),"url",url,"title",n.path("galTitle").asText(),
                "photographer",n.path("galPhotographer").asText(),"location",n.path("galPhotographyLocation").asText(),
                "month",n.path("galPhotographyMonth").asText(),"copyrightType",n.path("cpyrhtDivCd").asText("CHECK_SOURCE"),"source","한국관광공사 포토코리아"));
        }
        return photos;
    }
    public Map<String,Object> today() {
        var eligible=all().stream().filter(r->!photos(r.getCode()).isEmpty()).toList();
        if(eligible.isEmpty()) return Map.of("status","PENDING","message","오늘의 여행 사진을 준비하고 있어요.");
        long day=LocalDate.now(ZoneId.of("Asia/Seoul")).toEpochDay();
        var result=summary(eligible.get(Math.floorMod(day,eligible.size())));
        result.put("selectionType","EDITORIAL_ROTATION"); result.put("date",LocalDate.now(ZoneId.of("Asia/Seoul")).toString());
        return result;
    }
    public List<JsonNode> dataset(String code,String kind) { find(code);return store.read(code,kind); }
    public Map<String,Object> datasetResponse(String code,String kind,String source) {
        find(code);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",kind.equals("photos")?photos(code):store.read(code,kind));
        result.put("source",source);
        var fetched=store.fetchedAt(code,kind);result.put("fetchedAt",fetched);
        result.put("stale",fetched==null || fetched.isBefore(Instant.now().minus(Duration.ofDays(kind.equals("crowding")?2:7))));
        return result;
    }
    public static String safeImage(String url) {
        try {
            var uri=java.net.URI.create(url);
            if(!"tong.visitkorea.or.kr".equalsIgnoreCase(uri.getHost()))return null;
            if(!Set.of("http","https").contains(uri.getScheme()))return null;
            return url.replaceFirst("^http:","https:");
        }catch(Exception e){return null;}
    }
    public static boolean matchesPhotoRegion(String regionName,JsonNode photo) {
        String shortName=regionName.replaceFirst("[시군]$", "");
        String location=photo.path("galPhotographyLocation").asText();
        var local=java.util.regex.Pattern.compile("(^|[\\s,])"+java.util.regex.Pattern.quote(shortName)+"[시군]?(?=$|[\\s,])");
        if(local.matcher(location).find())return true;
        // Require the full administrative name in keywords; 남해바다/영월관 are not region matches.
        var keyword=java.util.regex.Pattern.compile("(^|[\\s,])"+java.util.regex.Pattern.quote(regionName)+"(?=$|[\\s,])");
        return keyword.matcher(photo.path("galSearchKeyword").asText()).find();
    }
}
