package kr.dasibom.application;

import java.text.Normalizer;
import java.util.*;

/** Choose one representative per place, even when filenames and photo titles differ. */
public final class PhotoSelection {
    private PhotoSelection() {}
    public static String key(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
            .replaceAll("[^가-힣a-z0-9]", "");
    }
    private static String placeKey(String value,String region){String k=key(value),r=key(region.replaceFirst("[시군]$",""));k=!r.isBlank()&&k.startsWith(r)?k.substring(r.length()):k;return k.replaceFirst("의(봄|여름|가을|겨울|아침|노을)$", "").replace("운조루고택","운조루").replaceAll("소금산(그랜드밸리|출렁다리|울렁다리)","소금산");}
    private static boolean containsPlace(String text,String place){return java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(place)+"(?![0-9])").matcher(text).find();}
    public static List<Map<String,Object>> diverse(List<Map<String,Object>> candidates) {
        // The shortest concrete place name wins over seasonal/photo-contest title variants.
        var venues = candidates.stream().filter(p -> "GALLERY".equals(p.get("photoType")))
            .map(p -> placeKey(String.valueOf(p.get("placeName")),String.valueOf(p.getOrDefault("regionName",""))))
            .filter(v -> v.length()>=3&&!v.contains("공모전")).distinct().sorted(Comparator.comparingInt(String::length)).toList();
        Set<String> places = new HashSet<>(), urls = new HashSet<>();
        List<Map<String,Object>> result = new ArrayList<>();
        for (var candidate : candidates) {
            var photo = new LinkedHashMap<>(candidate);
            String place = String.valueOf(photo.getOrDefault("placeName", photo.get("title")));
            String identity=placeKey(place,String.valueOf(photo.getOrDefault("regionName","")));
            String title=key(photo.get("title").toString()),keywords=key(String.valueOf(photo.getOrDefault("keywords",""))+" "+photo.get("location"));
            String match=venues.stream().filter(v->containsPlace(identity,v)||containsPlace(title,v)).findFirst().orElse(null);
            // Creative titles can name the work rather than the place; an explicit filming location resolves them.
            if(match==null||match.equals(identity)) {
                String location=key(String.valueOf(photo.getOrDefault("location","")));
                String located=venues.stream().filter(v->!v.equals(identity)&&containsPlace(location,v)).findFirst().orElse(null);
                if(located!=null)match=located;
            }
            if(match==null) match=venues.stream().filter(v->containsPlace(keywords,v)).findFirst().orElse(null);
            String resolved=match==null?identity:match;
            String url=String.valueOf(photo.get("url"));
            if(resolved.isBlank()||places.contains(resolved)||urls.contains(url))continue;
            places.add(resolved);urls.add(url);
            // Prefer a real source place label rather than a normalized machine key.
            if(match!=null){final String chosen=match;place=candidates.stream().filter(p->placeKey(String.valueOf(p.get("placeName")),String.valueOf(p.getOrDefault("regionName",""))).equals(chosen)).map(p->String.valueOf(p.get("placeName"))).findFirst().orElse(place);}
            photo.put("placeName",place);photo.put("placeKey",resolved);photo.remove("keywords");photo.remove("regionName");result.add(photo);
        }
        return List.copyOf(result);
    }
}
