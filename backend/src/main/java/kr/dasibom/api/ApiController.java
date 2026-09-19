package kr.dasibom.api;

import kr.dasibom.application.*;
import kr.dasibom.infrastructure.DatasetStore;
import jakarta.servlet.http.*;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import kr.dasibom.domain.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@Validated
public class ApiController {
    private final CatalogService catalog;private final DatasetStore store;private final SyncService sync;private final LikeRepository likes;
    private final String adminToken;private final String origin;private final boolean secure;
    public ApiController(CatalogService catalog,DatasetStore store,SyncService sync,LikeRepository likes,@Value("${dasibom.admin-token}") String adminToken,@Value("${dasibom.app-origin}") String origin,@Value("${dasibom.cookie-secure}") boolean secure){this.catalog=catalog;this.store=store;this.sync=sync;this.likes=likes;this.adminToken=adminToken;this.origin=origin;this.secure=secure;}
    @GetMapping("/home/today") public Object today(){return catalog.today();}
    @GetMapping("/regions") public Object regions(@RequestParam(defaultValue="") @Size(max=80) String q,@RequestParam(defaultValue="") String theme,@RequestParam(defaultValue="") String areaCode,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){
        var matches=catalog.all().stream().filter(r->r.getName().contains(q)||r.getAreaName().contains(q)).filter(r->theme.isBlank()||r.getTheme().equals(theme)).filter(r->areaCode.isBlank()||r.getAreaCode().equals(areaCode)).toList();
        return Map.of("items",matches.stream().skip((long)page*size).limit(size).map(catalog::summary).toList(),"total",matches.size(),"page",page,"size",size);
    }
    @GetMapping("/regions/{code}") public Object region(@PathVariable @Pattern(regexp="\\d{5}") String code){return catalog.summary(catalog.find(code));}
    @GetMapping("/regions/{code}/photos") public Object photos(@PathVariable String code){return catalog.datasetResponse(code,"photos","한국관광공사 포토코리아");}
    @GetMapping("/regions/{code}/places") public Object places(@PathVariable String code){return catalog.datasetResponse(code,"places","한국관광공사 국문 관광정보");}
    @GetMapping("/regions/{code}/crowding") public Object crowd(@PathVariable String code){return catalog.datasetResponse(code,"crowding","한국관광공사 관광지 집중률 예측");}
    @GetMapping("/regions/{code}/metrics") public Object metrics(@PathVariable String code){catalog.find(code);return Map.of("status","PENDING","items",List.of(),"message","전국 동일 기준월 데이터가 모이면 공개합니다.");}
    @GetMapping("/map/regions") public Object map(){return Map.of("items",catalog.all().stream().map(catalog::summary).toList(),"geometryType","CENTROID");}
    @GetMapping("/data-status") public Object status(){return Map.of("datasets",store.status(),"syncRunning",sync.running(),"coverage","EDITORIAL_8_REGIONS");}
    @GetMapping("/regions/{code}/like") public Object likeStatus(@PathVariable String code,@CookieValue(name="dasibom_visitor",defaultValue="") String visitor){catalog.find(code);return reaction(code,visitor);}
    @PutMapping("/regions/{code}/like") public Object like(@PathVariable String code,@CookieValue(name="dasibom_visitor",defaultValue="") String visitor,HttpServletRequest request,HttpServletResponse response){
        sameOrigin(request);catalog.find(code);
        try{UUID.fromString(visitor);}catch(Exception e){visitor=UUID.randomUUID().toString();response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("dasibom_visitor",visitor).httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(31536000).build().toString());}
        likes.like(code,visitor);return reaction(code,visitor);
    }
    @DeleteMapping("/regions/{code}/like") public Object unlike(@PathVariable String code,@CookieValue(name="dasibom_visitor",defaultValue="") String visitor,HttpServletRequest request){sameOrigin(request);catalog.find(code);likes.unlike(code,visitor);return reaction(code,visitor);}
    private Object reaction(String code,String visitor){return Map.of("liked",likes.existsById(new RegionLike.Id(code,visitor)),"count",likes.countByIdRegionCode(code));}
    private void sameOrigin(HttpServletRequest request){if(!origin.equals(request.getHeader("Origin")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"요청 출처가 올바르지 않습니다.");}
    @PostMapping("/admin/sync/catalog") public ResponseEntity<?> sync(@RequestHeader(value="Authorization",defaultValue="") String token){admin(token);if(!sync.start())throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 수집 중입니다.");return ResponseEntity.accepted().body(Map.of("status","ACCEPTED"));}
    @GetMapping("/admin/sync-runs") public Object runs(@RequestHeader(value="Authorization",defaultValue="") String token){admin(token);return Map.of("items",sync.history());}
    private void admin(String provided){if(adminToken.length()<24||!MessageDigest.isEqual(("Bearer "+adminToken).getBytes(StandardCharsets.UTF_8),provided.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"관리자 인증이 필요합니다.");}
}
