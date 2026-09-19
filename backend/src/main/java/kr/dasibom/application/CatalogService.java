package kr.dasibom.application;

import com.fasterxml.jackson.databind.JsonNode;
import kr.dasibom.domain.*;
import kr.dasibom.infrastructure.TourApiClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class CatalogService {
    private final RegionRepository regions;
    private final LikeRepository likes;
    private final TourApiClient api;
    public CatalogService(RegionRepository regions, LikeRepository likes, TourApiClient api) { this.regions=regions;this.likes=likes;this.api=api; }
    public Region find(String code) { return regions.findById(code).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"지역을 찾을 수 없습니다.")); }
    public List<Region> all() { return regions.findAll().stream().sorted(Comparator.comparing(Region::getCode)).toList(); }
    public List<Region> editorial() { return all().stream().filter(r->r.getLatitude()!=null&&r.getLongitude()!=null&&r.getAnchorPlace()!=null).toList(); }
    public Map<String,Object> summary(Region r,boolean withHero) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("code",r.getCode());result.put("name",r.getName());result.put("areaCode",r.getAreaCode());result.put("areaName",r.getAreaName());
        result.put("tagline",r.getTagline());result.put("theme",r.getTheme());result.put("latitude",r.getLatitude());result.put("longitude",r.getLongitude());
        result.put("anchorPlace",r.getAnchorPlace());result.put("source","출처: ⓒ한국관광공사");
        var hero=withHero?heroPhoto(r):null;result.put("heroPhoto",hero);
        result.put("dataStatus",withHero?(hero==null?"UNAVAILABLE":"LIVE"):"NOT_REQUESTED");result.put("hiddenScore",null);result.put("attractionScore",null);
        result.put("likes",withHero?likes.countByIdRegionCode(r.getCode()):0);
        return result;
    }
    public Map<String,Object> summary(Region r) { return summary(r,true); }
    public List<Map<String,Object>> photos(String code) { return photos(find(code),48,36); }
    private Map<String,Object> heroPhoto(Region region) {
        var photos=photos(region,12,1);return photos.isEmpty()?null:photos.getFirst();
    }
    private List<Map<String,Object>> photos(Region region,int rows,int limit) {
        List<Map<String,Object>> photos=new ArrayList<>(); Set<String> urls=new HashSet<>();
        for(JsonNode n:api.fetch("PhotoGalleryService1/gallerySearchList1",Map.of("keyword",region.getName(),"arrange","C"),rows)) {
            if(!matchesPhotoRegion(region.getName(),n))continue;
            String url=safeImage(n.path("galWebImageUrl").asText());
            if(url==null || !urls.add(url) || !api.imageAvailable(url))continue;
            photos.add(Map.of("id",n.path("galContentId").asText(),"url",url,"title",n.path("galTitle").asText(),
                "photographer",n.path("galPhotographer").asText(),"location",n.path("galPhotographyLocation").asText(),
                "month",n.path("galPhotographyMonth").asText(),"copyrightType",n.path("cpyrhtDivCd").asText("CHECK_SOURCE"),"source","출처: ⓒ한국관광콘텐츠랩"));
            if(photos.size()>=limit)break;
        }
        return photos;
    }
    public Map<String,Object> today() {
        var eligible=editorial();
        if(eligible.isEmpty()) return Map.of("status","PENDING","message","오늘의 여행 사진을 준비하고 있어요.");
        long day=LocalDate.now(ZoneId.of("Asia/Seoul")).toEpochDay();
        for(int offset=0;offset<eligible.size();offset++) {
            var result=summary(eligible.get(Math.floorMod(day+offset,eligible.size())));
            if(result.get("heroPhoto")!=null){result.put("selectionType","EDITORIAL_ROTATION");result.put("date",LocalDate.now(ZoneId.of("Asia/Seoul")).toString());return result;}
        }
        return Map.of("status","PENDING","message","오늘의 여행 사진을 준비하고 있어요.");
    }
    public List<JsonNode> places(String code) {
        Region r=find(code);return api.fetch("KorService2/areaBasedList2",Map.of("lDongRegnCd",r.getAreaCode(),"lDongSignguCd",r.getCode().substring(2),"arrange","Q","contentTypeId","12"),12);
    }
    public List<JsonNode> crowding(String code) {
        Region r=find(code);Map<String,String> params=new LinkedHashMap<>();params.put("areaCd",r.getAreaCode());params.put("signguCd",r.getCode());
        if(r.getAnchorPlace()!=null&&!r.getAnchorPlace().isBlank())params.put("tAtsNm",r.getAnchorPlace());
        return api.fetch("TatsCnctrRateService/tatsCnctrRatedList",params,30);
    }
    public Map<String,Object> liveResponse(List<?> items,String source) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",items);result.put("source",source);result.put("fetchedAt",Instant.now());result.put("stale",false);result.put("mode","LIVE");
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
