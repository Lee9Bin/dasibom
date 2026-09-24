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
        Region region=find(code);
        // A bounded discovery set avoids pages filled by one photo shoot. Public responses are not persisted.
        var params=Map.of("keyword",region.getName(),"arrange","C");
        var first=api.fetchPage("PhotoGalleryService1/gallerySearchList1",params,100,1);
        List<JsonNode> rows=new ArrayList<>(first.items());
        List<Map<String,Object>> candidates=new ArrayList<>();
        try { candidates.addAll(awardPhotos(region)); } catch (TourApiClient.TourApiException ignored) { /* Gallery remains usable. */ }
        try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Callable<List<JsonNode>>> tasks=new ArrayList<>();
            for(int n=2;n<=Math.min(3,(first.totalCount()+99)/100);n++) {
                final int sourcePage=n;
                tasks.add(()->api.fetchPage("PhotoGalleryService1/gallerySearchList1",params,100,sourcePage).items());
            }
            try { for(var future:executor.invokeAll(tasks,9,java.util.concurrent.TimeUnit.SECONDS))
                try { rows.addAll(future.get()); } catch(Exception ignored) { }
            } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        for(JsonNode n:rows) if(matchesPhotoRegion(region,n)&&safeImage(n.path("galWebImageUrl").asText())!=null) candidates.add(photo(n,region.getName()));
        var selected=PhotoSelection.diverse(candidates);
        int from=Math.min(selected.size(),(page-1)*size),to=Math.min(selected.size(),from+size);
        List<Map<String,Object>> visible=new ArrayList<>();
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(6)) {
            var tasks=selected.subList(from,to).stream().<java.util.concurrent.Callable<Map<String,Object>>>map(p->()->api.imageAvailable(String.valueOf(p.get("url")))?p:null).toList();
            try { for(var future:executor.invokeAll(tasks,7,java.util.concurrent.TimeUnit.SECONDS))
                try { var item=future.get(); if(item!=null)visible.add(item); } catch(Exception ignored) { }
            } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return new PhotoPage(List.copyOf(visible),page,size,selected.size(),to<selected.size());
    }
    private Map<String,Object> heroPhoto(Region region) {
        var photos=photos(region,12,1);return photos.isEmpty()?null:photos.getFirst();
    }
    private List<Map<String,Object>> photos(Region region,int rows,int limit) {
        List<Map<String,Object>> photos=new ArrayList<>(); Set<String> urls=new HashSet<>();
        for(JsonNode n:api.fetch("PhotoGalleryService1/gallerySearchList1",Map.of("keyword",region.getName(),"arrange","C"),rows)) {
            if(!matchesPhotoRegion(region,n))continue;
            String url=safeImage(n.path("galWebImageUrl").asText());
            if(url==null || !urls.add(url) || !api.imageAvailable(url))continue;
            photos.add(Map.of("id",n.path("galContentId").asText(),"url",url,"title",n.path("galTitle").asText(),
                "photographer",n.path("galPhotographer").asText(),"location",n.path("galPhotographyLocation").asText(),
                "month",n.path("galPhotographyMonth").asText(),"copyrightType",n.path("cpyrhtDivCd").asText("CHECK_SOURCE"),"source","출처: ⓒ한국관광콘텐츠랩"));
            if(photos.size()>=limit)break;
        }
        return photos;
    }
    private Map<String,Object> photo(JsonNode n,String regionName){
        Map<String,Object> p=new LinkedHashMap<>();p.put("id",n.path("galContentId").asText());p.put("url",safeImage(n.path("galWebImageUrl").asText()));p.put("title",n.path("galTitle").asText());p.put("photographer",n.path("galPhotographer").asText());p.put("location",n.path("galPhotographyLocation").asText());p.put("month",n.path("galPhotographyMonth").asText());p.put("copyrightType",n.path("cpyrhtDivCd").asText("CHECK_SOURCE"));p.put("photoType","GALLERY");String venue=n.path("galSearchKeyword").asText("").split(",")[0].trim();p.put("placeName",venue.isBlank()||venue.contains("공모전")||venue.contains("사진기자단")?n.path("galTitle").asText():venue);p.put("keywords",n.path("galSearchKeyword").asText());p.put("regionName",regionName);p.put("source","출처: ⓒ한국관광콘텐츠랩");return p;
    }
    public List<Map<String,Object>> awardPhotos(Region region){
        return awardPhotos(region,api.fetch("PhokoAwrdService/phokoAwrdList",Map.of("lDongRegnCd",RegionCodes.currentCode(region.getCode()).substring(0,2),"arrange","C"),100));
    }
    private List<Map<String,Object>> awardPhotos(Region region,List<JsonNode> rows){
        List<Map<String,Object>> result=new ArrayList<>();
        for(JsonNode n:rows){
            if(!matchesAdministrativeName(region.getName(),n.path("koFilmst").asText()+" "+n.path("koKeyWord").asText()))continue;
            String url=safeImage(n.path("orgImage").asText());if(url==null)continue;
            Map<String,Object> p=new LinkedHashMap<>();p.put("id","award-"+n.path("contentId").asText());p.put("url",url);p.put("title",n.path("koTitle").asText(region.getName()+" 관광사진"));p.put("photographer",n.path("koCmanNm").asText());p.put("location",n.path("koFilmst").asText());p.put("month",normalizeMonth(n.path("filmDay").asText()));p.put("copyrightType",n.path("cpyrhtDivCd").asText("CHECK_SOURCE"));p.put("photoType","AWARD");p.put("placeName",n.path("koTitle").asText());p.put("award",n.path("koWnprzDiz").asText());p.put("source","출처: ⓒ한국관광콘텐츠랩");result.add(p);
        }
        return result;
    }
    public Map<String,Object> today() {
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Seoul"));
        var eligible=all().stream().filter(r->!"NONE".equals(r.getPopulationStatus())||r.isHalfPrice()).sorted(Comparator.comparing(Region::getCode)).toList();
        List<Map.Entry<Region,Map<String,Object>>> awarded=new ArrayList<>();
        VisitorInsightService.Snapshot snapshot=new VisitorInsightService.Snapshot(null,Map.of(),Map.of());
        try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
            var visitorFuture=executor.submit(visitors::latest);
            var futures=eligible.stream().map(r->RegionCodes.currentCode(r.getCode()).substring(0,2)).distinct().collect(java.util.stream.Collectors.toMap(a->a,a->executor.submit(()->api.fetch("PhokoAwrdService/phokoAwrdList",Map.of("lDongRegnCd",a,"arrange","C"),100))));Map<String,List<JsonNode>> byArea=new HashMap<>();for(var future:futures.entrySet())try{byArea.put(future.getKey(),future.getValue().get());}catch(Exception ignored){}
            for(Region region:eligible){var photos=awardPhotos(region,byArea.getOrDefault(RegionCodes.currentCode(region.getCode()).substring(0,2),List.of()));if(!photos.isEmpty())awarded.add(Map.entry(region,photos.getFirst()));}
            try{snapshot=visitorFuture.get();}catch(Exception ignored){}
        }
        final var visitorSnapshot=snapshot;
        awarded.sort(Comparator.<Map.Entry<Region,Map<String,Object>>>comparingInt(e->visitorSnapshot.hiddenScores().getOrDefault(e.getKey().getCode(),0)).reversed().thenComparing(e->e.getKey().getCode()));
        boolean hasVisitorData=!snapshot.hiddenScores().isEmpty();
        if(hasVisitorData) awarded.removeIf(e->visitorSnapshot.hiddenScores().getOrDefault(e.getKey().getCode(),-1)<50);
        if(!awarded.isEmpty()){
            var selected=awarded.get(Math.floorMod(today.toEpochDay(),awarded.size()));var result=summary(selected.getKey(),false);result.put("heroPhoto",selected.getValue());result.put("dataStatus","LIVE");result.put("hiddenScore",snapshot.hiddenScores().get(selected.getKey().getCode()));result.put("selectionType",hasVisitorData?"LOW_VISITOR_AWARD_ROTATION":"AWARD_ROTATION");result.put("selectionReason",hasVisitorData?"정책지역 중 방문량이 비교 지역의 하위 절반에 속하고 관광사진 수상작이 있는 곳":"방문량 자료를 확인하지 못해 정책지역의 관광사진 수상작을 기준으로 소개합니다.");result.put("visitorDataAsOf",snapshot.asOf());result.put("candidateCount",awarded.size());result.put("date",today.toString());return result;
        }
        return Map.of("status","PENDING","message","오늘의 여행 사진을 준비하고 있어요.");
    }
    public List<JsonNode> places(String code) {
        Region r=find(code);return api.fetch("KorService2/areaBasedList2",Map.of("lDongRegnCd",RegionCodes.currentCode(r.getCode()).substring(0,2),"lDongSignguCd",RegionCodes.currentCode(r.getCode()).substring(2),"arrange","Q","contentTypeId","12"),12);
    }
    public List<JsonNode> crowding(String code) {
        Region r=find(code);Map<String,String> params=new LinkedHashMap<>();params.put("areaCd",RegionCodes.currentCode(r.getCode()).substring(0,2));params.put("signguCd",RegionCodes.currentCode(r.getCode()));
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
    private boolean matchesPhotoRegion(Region region,JsonNode photo) {
        if(!matchesPhotoRegion(region.getName(),photo))return false;
        if(!region.getName().endsWith("구"))return true;
        String location=photo.path("galPhotographyLocation").asText()+" "+photo.path("galSearchKeyword").asText();
        String area=region.getAreaName().replace("특별자치도","").replace("특별자치시","").replace("광역시","").replace("특별시","");
        return location.contains(area)||(region.getAreaCode().equals("29")&&location.contains("전남광주"));
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
