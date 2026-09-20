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
    private final VisitorInsightService visitors;
    public CatalogService(RegionRepository regions, LikeRepository likes, TourApiClient api,VisitorInsightService visitors) { this.regions=regions;this.likes=likes;this.api=api;this.visitors=visitors; }
    public Region find(String code) { return regions.findById(code).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"지역을 찾을 수 없습니다.")); }
    public List<Region> all() { return regions.findAll().stream().sorted(Comparator.comparing(Region::getCode)).toList(); }
    public List<Region> editorial() { return all().stream().filter(r->r.getLatitude()!=null&&r.getLongitude()!=null&&r.getAnchorPlace()!=null).toList(); }
    public Map<String,Object> summary(Region r,boolean withHero) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("code",r.getCode());result.put("name",r.getName());result.put("areaCode",r.getAreaCode());result.put("areaName",r.getAreaName());
        result.put("tagline",r.getTagline());result.put("theme",r.getTheme());result.put("latitude",r.getLatitude());result.put("longitude",r.getLongitude());
        result.put("anchorPlace",r.getAnchorPlace());result.put("populationStatus",r.getPopulationStatus());result.put("halfPrice",r.isHalfPrice());result.put("source","출처: ⓒ한국관광공사");
        var hero=withHero?heroPhoto(r):null;result.put("heroPhoto",hero);
        result.put("dataStatus",withHero?(hero==null?"UNAVAILABLE":"LIVE"):"NOT_REQUESTED");result.put("hiddenScore",null);result.put("attractionScore",null);
        result.put("likes",withHero?likes.countByIdRegionCode(r.getCode()):0);
        return result;
    }
    public Map<String,Object> summary(Region r) { return summary(r,true); }
    public Map<String,Object> mapRegions(){
        VisitorInsightService.Snapshot snapshot;
        try{snapshot=visitors.latest();}catch(Exception e){snapshot=new VisitorInsightService.Snapshot(null,Map.of(),Map.of());}
        List<Map<String,Object>> items=new ArrayList<>();
        for(Region r:all()){var item=summary(r,false);item.put("hiddenScore",snapshot.hiddenScores().get(r.getCode()));items.add(item);}
        return Map.of("items",items,"visitorDataAsOf",Optional.ofNullable(snapshot.asOf()).map(Object::toString).orElse(""),"geometryType","SGG_BOUNDARY","source","출처: ⓒ한국관광공사");
    }
    public List<Map<String,Object>> photos(String code) { return photoPage(code,1,36).items(); }
    public record PhotoPage(List<Map<String,Object>> items,int page,int size,int total,boolean hasMore) {}
    public PhotoPage photoPage(String code,int page,int size) {
        Region region=find(code);int fetchRows=Math.min(100,Math.max(size*2,24));
        var raw=api.fetchPage("PhotoGalleryService1/gallerySearchList1",Map.of("keyword",region.getName(),"arrange","C"),fetchRows,page);
        List<Map<String,Object>> result=new ArrayList<>();Set<String> urls=new HashSet<>();
        if(page==1)for(var award:awardPhotos(region)){String url=String.valueOf(award.get("url"));if(urls.add(url))result.add(award);if(result.size()>=size)break;}
        for(JsonNode n:raw.items()){
            if(!matchesPhotoRegion(region.getName(),n))continue;
            String url=safeImage(n.path("galWebImageUrl").asText());if(url==null||!urls.add(url)||!api.imageAvailable(url))continue;
            result.add(photo(n));if(result.size()>=size)break;
        }
        long awardCount=page==1?result.stream().filter(p->"AWARD".equals(p.get("photoType"))).count():0;
        int total=(int)Math.min(Integer.MAX_VALUE,raw.totalCount()+awardCount);
        return new PhotoPage(List.copyOf(result),page,size,total,page*fetchRows<raw.totalCount());
    }
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
    private Map<String,Object> photo(JsonNode n){
        Map<String,Object> p=new LinkedHashMap<>();p.put("id",n.path("galContentId").asText());p.put("url",safeImage(n.path("galWebImageUrl").asText()));p.put("title",n.path("galTitle").asText());p.put("photographer",n.path("galPhotographer").asText());p.put("location",n.path("galPhotographyLocation").asText());p.put("month",n.path("galPhotographyMonth").asText());p.put("copyrightType",n.path("cpyrhtDivCd").asText("CHECK_SOURCE"));p.put("photoType","GALLERY");p.put("source","출처: ⓒ한국관광콘텐츠랩");return p;
    }
    public List<Map<String,Object>> awardPhotos(Region region){
        return awardPhotos(region,api.fetch("PhokoAwrdService/phokoAwrdList",Map.of("lDongRegnCd",region.getAreaCode(),"arrange","C"),100));
    }
    private List<Map<String,Object>> awardPhotos(Region region,List<JsonNode> rows){
        List<Map<String,Object>> result=new ArrayList<>();
        for(JsonNode n:rows){
            if(!matchesAdministrativeName(region.getName(),n.path("koFilmst").asText()+" "+n.path("koKeyWord").asText()))continue;
            String url=safeImage(n.path("orgImage").asText());if(url==null)continue;
            Map<String,Object> p=new LinkedHashMap<>();p.put("id","award-"+n.path("contentId").asText());p.put("url",url);p.put("title",n.path("koTitle").asText(region.getName()+" 관광사진"));p.put("photographer",n.path("koCmanNm").asText());p.put("location",n.path("koFilmst").asText());p.put("month",normalizeMonth(n.path("filmDay").asText()));p.put("copyrightType",n.path("cpyrhtDivCd").asText("CHECK_SOURCE"));p.put("photoType","AWARD");p.put("award",n.path("koWnprzDiz").asText());p.put("source","출처: ⓒ한국관광콘텐츠랩");result.add(p);
        }
        return result;
    }
    public Map<String,Object> today() {
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Seoul"));
        VisitorInsightService.Snapshot snapshot;
        try{snapshot=visitors.latest();}catch(Exception e){snapshot=new VisitorInsightService.Snapshot(null,Map.of(),Map.of());}
        final var visitorSnapshot=snapshot;
        var eligible=all().stream().filter(r->!"NONE".equals(r.getPopulationStatus())||r.isHalfPrice()).sorted(Comparator.comparing(Region::getCode)).toList();
        List<Map.Entry<Region,Map<String,Object>>> awarded=new ArrayList<>();
        List<JsonNode> globalAwards=List.of();try{globalAwards=api.fetch("PhokoAwrdService/phokoAwrdSyncList",Map.of("showflag","1"),100);}catch(Exception ignored){}
        if(!globalAwards.isEmpty()){
            for(Region region:eligible){var photos=awardPhotos(region,globalAwards);if(!photos.isEmpty())awarded.add(Map.entry(region,photos.getFirst()));}
        }else try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
            var futures=eligible.stream().map(Region::getAreaCode).distinct().collect(java.util.stream.Collectors.toMap(a->a,a->executor.submit(()->api.fetch("PhokoAwrdService/phokoAwrdList",Map.of("lDongRegnCd",a,"arrange","C"),100))));Map<String,List<JsonNode>> byArea=new HashMap<>();for(var future:futures.entrySet())try{byArea.put(future.getKey(),future.getValue().get());}catch(Exception ignored){}
            for(Region region:eligible){var photos=awardPhotos(region,byArea.getOrDefault(region.getAreaCode(),List.of()));if(!photos.isEmpty())awarded.add(Map.entry(region,photos.getFirst()));}
        }
        awarded.sort(Comparator.<Map.Entry<Region,Map<String,Object>>>comparingInt(e->visitorSnapshot.hiddenScores().getOrDefault(e.getKey().getCode(),0)).reversed().thenComparing(e->e.getKey().getCode()));
        if(!awarded.isEmpty()){
            var selected=awarded.get(Math.floorMod(today.toEpochDay(),awarded.size()));var result=summary(selected.getKey(),false);result.put("heroPhoto",selected.getValue());result.put("dataStatus","LIVE");result.put("hiddenScore",snapshot.hiddenScores().get(selected.getKey().getCode()));result.put("selectionType","LOW_VISITOR_AWARD_ROTATION");result.put("selectionReason","인구감소·관심 또는 반값여행 지역 중 관광사진 수상작이 있고 방문량이 낮은 후보군");result.put("visitorDataAsOf",snapshot.asOf());result.put("candidateCount",awarded.size());result.put("date",today.toString());return result;
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
    public static boolean matchesAdministrativeName(String regionName,String text){
        String shortName=regionName.replaceFirst("[시군구]$","");
        return java.util.regex.Pattern.compile("(^|[\\s,·()])"+java.util.regex.Pattern.quote(shortName)+"[시군구]?(?=$|[\\s,·()])").matcher(text).find();
    }
    private static String normalizeMonth(String value){String digits=value.replaceAll("[^0-9]","");return digits.length()>=6?digits.substring(0,6):digits;}
}
