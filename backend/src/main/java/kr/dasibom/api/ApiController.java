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
    private final CatalogService catalog;private final LikeRepository likes;
    private final String origin;private final boolean secure;
    public ApiController(CatalogService catalog,LikeRepository likes,@Value("${dasibom.app-origin}") String origin,@Value("${dasibom.cookie-secure}") boolean secure){this.catalog=catalog;this.likes=likes;this.origin=origin;this.secure=secure;}
    @GetMapping("/home/today") public Object today(){return catalog.today();}
    @GetMapping("/regions") public Object regions(@RequestParam(defaultValue="") @Size(max=80) String q,@RequestParam(defaultValue="") String theme,@RequestParam(defaultValue="") String areaCode,@RequestParam(defaultValue="false") boolean featured,@RequestParam(defaultValue="false") boolean withHero,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(300) int size){
        var source=featured?catalog.editorial():catalog.all();
        var matches=source.stream().filter(r->r.getName().contains(q)||r.getAreaName().contains(q)).filter(r->theme.isBlank()||r.getTheme().equals(theme)).filter(r->areaCode.isBlank()||r.getAreaCode().equals(areaCode)).toList();
        return Map.of("items",matches.stream().skip((long)page*size).limit(size).map(r->catalog.summary(r,withHero)).toList(),"total",matches.size(),"page",page,"size",size);
    }
    @GetMapping("/regions/{code}") public Object region(@PathVariable @Pattern(regexp="\\d{5}") String code,@RequestParam(defaultValue="true") boolean withHero){return catalog.summary(catalog.find(code),withHero);}
    @GetMapping("/regions/{code}/photos") public Object photos(@PathVariable String code){return catalog.liveResponse(catalog.photos(code),"출처: ⓒ한국관광콘텐츠랩");}
    @GetMapping("/regions/{code}/places") public Object places(@PathVariable String code){return catalog.liveResponse(catalog.places(code),"출처: ⓒ한국관광공사");}
    @GetMapping("/regions/{code}/crowding") public Object crowd(@PathVariable String code){return catalog.liveResponse(catalog.crowding(code),"출처: ⓒ한국관광공사");}
    @GetMapping("/regions/{code}/metrics") public Object metrics(@PathVariable String code){catalog.find(code);return Map.of("status","PENDING","items",List.of(),"message","전국 동일 기준월 데이터가 모이면 공개합니다.");}
    @GetMapping("/map/regions") public Object map(){return Map.of("items",catalog.editorial().stream().map(r->catalog.summary(r,false)).toList(),"geometryType","VERIFIED_CENTROID");}
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
