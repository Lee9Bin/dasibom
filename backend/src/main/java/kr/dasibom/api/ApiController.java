package kr.dasibom.api;

import kr.dasibom.application.*;
import jakarta.servlet.http.*;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import kr.dasibom.domain.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@Validated
public class ApiController {
    private final CatalogService catalog;private final LikeRepository likes;private final MetricService metrics;private final RecommendationService recommendations;
    private final String origin;private final boolean secure;
    public ApiController(CatalogService catalog,LikeRepository likes,MetricService metrics,RecommendationService recommendations,@Value("${dasibom.app-origin}") String origin,@Value("${dasibom.cookie-secure}") boolean secure){this.catalog=catalog;this.likes=likes;this.metrics=metrics;this.recommendations=recommendations;this.origin=origin;this.secure=secure;}
    @GetMapping("/home/today") public Object today(){return catalog.today();}
    @GetMapping("/regions") public Object regions(@RequestParam(defaultValue="") @Size(max=80) String q,@RequestParam(defaultValue="") String theme,@RequestParam(defaultValue="") String areaCode,@RequestParam(defaultValue="") String policy,@RequestParam(defaultValue="false") boolean featured,@RequestParam(defaultValue="false") boolean withHero,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(300) int size){
        var source=featured?catalog.editorial():catalog.all();
        var matches=source.stream().filter(r->r.getName().contains(q)||r.getAreaName().contains(q)).filter(r->theme.isBlank()||r.getTheme().equals(theme)).filter(r->areaCode.isBlank()||r.getAreaCode().equals(areaCode)).filter(r->policy.isBlank()||("HALF_PRICE".equals(policy)?r.isHalfPrice():r.getPopulationStatus().equals(policy))).toList();
        return Map.of("items",matches.stream().skip((long)page*size).limit(size).map(r->catalog.summary(r,withHero)).toList(),"total",matches.size(),"page",page,"size",size);
    }
    @GetMapping("/regions/{code}") public Object region(@PathVariable @Pattern(regexp="\\d{5}") String code,@RequestParam(defaultValue="true") boolean withHero){return catalog.summary(catalog.find(code),withHero);}
    @GetMapping("/regions/{code}/photos") public Object photos(@PathVariable @Pattern(regexp="\\d{5}") String code,@RequestParam(defaultValue="1") @Min(1) @Max(1000) int page,@RequestParam(defaultValue="12") @Min(1) @Max(36) int size){var p=catalog.photoPage(code,page,size);return Map.of("items",p.items(),"page",p.page(),"size",p.size(),"total",p.total(),"hasMore",p.hasMore(),"source","출처: ⓒ한국관광콘텐츠랩","fetchedAt",java.time.Instant.now(),"mode","LIVE");}
    @GetMapping("/regions/{code}/places") public Object places(@PathVariable String code){return catalog.liveResponse(catalog.places(code),"출처: ⓒ한국관광공사");}
    @GetMapping("/regions/{code}/crowding") public Object crowd(@PathVariable String code){return catalog.liveResponse(catalog.crowding(code),"출처: ⓒ한국관광공사");}
    @GetMapping("/regions/{code}/metrics") public Object metrics(@PathVariable @Pattern(regexp="\\d{5}") String code){return metrics.metrics(catalog.find(code));}
    @GetMapping("/regions/{code}/recommendations") public Object recommendations(@PathVariable @Pattern(regexp="\\d{5}") String code){return recommendations.recommendations(catalog.find(code));}
    @GetMapping("/map/regions") public Object map(){return catalog.mapRegions();}
    @GetMapping("/filters") public Object filters(){var all=catalog.all();return Map.of("populationDecline",all.stream().filter(r->"DECLINING".equals(r.getPopulationStatus())).count(),"populationInterest",all.stream().filter(r->"INTEREST".equals(r.getPopulationStatus())).count(),"halfPrice",all.stream().filter(Region::isHalfPrice).count(),"halfPriceAsOf","2026-09-20");}
    @GetMapping("/data-status") public Object status(){return Map.of("mode","LIVE","persistentPublicData",false,"regionCount",catalog.all().size(),"featuredRegionCount",catalog.editorial().size(),"checkedAt",java.time.Instant.now(),"source","출처: ⓒ한국관광공사");}
    @GetMapping("/regions/{code}/like") public Object likeStatus(@PathVariable String code,@CookieValue(name="dasibom_visitor",defaultValue="") String visitor){catalog.find(code);return reaction(code,visitor);}
    @PutMapping("/regions/{code}/like") public Object like(@PathVariable String code,@CookieValue(name="dasibom_visitor",defaultValue="") String visitor,HttpServletRequest request,HttpServletResponse response){
        sameOrigin(request);catalog.find(code);
        try{UUID.fromString(visitor);}catch(Exception e){visitor=UUID.randomUUID().toString();response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("dasibom_visitor",visitor).httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(31536000).build().toString());}
        likes.like(code,visitor);return reaction(code,visitor);
    }
    @DeleteMapping("/regions/{code}/like") public Object unlike(@PathVariable String code,@CookieValue(name="dasibom_visitor",defaultValue="") String visitor,HttpServletRequest request){sameOrigin(request);catalog.find(code);likes.unlike(code,visitor);return reaction(code,visitor);}
    private Object reaction(String code,String visitor){return Map.of("liked",likes.existsById(new RegionLike.Id(code,visitor)),"count",likes.countByIdRegionCode(code));}
    private void sameOrigin(HttpServletRequest request){if(!origin.equals(request.getHeader("Origin")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"요청 출처가 올바르지 않습니다.");}
}
