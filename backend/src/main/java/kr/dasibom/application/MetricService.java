package kr.dasibom.application;

import com.fasterxml.jackson.databind.JsonNode;
import kr.dasibom.domain.Region;
import kr.dasibom.infrastructure.TourApiClient;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MetricService {
    private static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("1101","자연경관"),Map.entry("1102","휴양·힐링"),Map.entry("1103","미식"),Map.entry("1104","역사관광"),
        Map.entry("1105","레저스포츠"),Map.entry("1106","쇼핑"),Map.entry("1107","숙박"),Map.entry("1108","체험"),
        Map.entry("1109","축제·공연"),Map.entry("1110","캠핑"),Map.entry("1111","카페"),Map.entry("1112","테마관광"),
        Map.entry("1201","문화유산"),Map.entry("1202","자연유산"),Map.entry("1203","문화시설"),Map.entry("1204","지역문화"),Map.entry("1205","생태자원")
    );
    private static final Map<String,String> THEMES=Map.ofEntries(
        Map.entry("1101","NATURE"),Map.entry("1202","NATURE"),Map.entry("1205","NATURE"),Map.entry("1102","HEALING"),
        Map.entry("1103","FOOD"),Map.entry("1106","FOOD"),Map.entry("1111","FOOD"),Map.entry("1104","CULTURE"),
        Map.entry("1201","CULTURE"),Map.entry("1203","CULTURE"),Map.entry("1204","CULTURE"),Map.entry("1107","STAY"),
        Map.entry("1110","STAY"),Map.entry("1105","ACTIVITY"),Map.entry("1108","ACTIVITY"),Map.entry("1109","ACTIVITY"),Map.entry("1112","ACTIVITY")
    );
    private final TourApiClient api;
    public MetricService(TourApiClient api){this.api=api;}

    public Map<String,Object> metrics(Region region){
        YearMonth month=YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1);
        for(int i=0;i<12;i++){
            String baseYm=month.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM"));
            Map<String,String> params=Map.of("baseYm",baseYm,"areaCd",region.getAreaCode(),"signguCd",region.getCode());
            List<Map<String,Object>> metrics=new ArrayList<>();
            collect(metrics,api.fetch("AreaTarResDemService/areaTarSvcDemList",params,100),false);
            collect(metrics,api.fetch("AreaTarResDemService/areaCulResDemList",params,100),true);
            if(!metrics.isEmpty())return result("LIVE",baseYm,metrics,null);
        }
        return result("UNAVAILABLE",null,List.of(),"한국관광공사 원천 API에 현재 조회 가능한 자원 수요 데이터가 없습니다.");
    }
    private void collect(List<Map<String,Object>> out,List<JsonNode> rows,boolean culture){
        for(JsonNode row:rows){
            String code=first(row,culture?new String[]{"culResDemLclsCd","culResDemMclsCd","culResDemCd"}:new String[]{"tarSvcDemLclsCd","tarSvcDemMclsCd","tarSvcDemCd"});
            if(!LABELS.containsKey(code))continue;
            Double value=number(row,culture?new String[]{"culResDemIxVal","culResDemVal"}:new String[]{"tarSvcDemIxVal","tarSvcDemVal"});
            if(value==null)continue;
            out.add(Map.of("code",code,"name",firstOr(row,LABELS.get(code),culture?new String[]{"culResDemLclsNm","culResDemMclsNm"}:new String[]{"tarSvcDemLclsNm","tarSvcDemMclsNm"}),"theme",THEMES.get(code),"value",Math.max(0,Math.min(100,value))));
        }
    }
    private static Map<String,Object> result(String status,String baseYm,List<Map<String,Object>> metrics,String message){
        Map<String,List<Double>> groups=new LinkedHashMap<>();for(String theme:List.of("NATURE","HEALING","FOOD","CULTURE","STAY","ACTIVITY"))groups.put(theme,new ArrayList<>());
        metrics.forEach(m->groups.get(String.valueOf(m.get("theme"))).add((Double)m.get("value")));
        List<Map<String,Object>> radar=groups.entrySet().stream().map(e->Map.<String,Object>of("theme",e.getKey(),"value",e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0))).toList();
        Map<String,Object> result=new LinkedHashMap<>();result.put("status",status);result.put("baseYm",baseYm);result.put("items",metrics);result.put("radar",radar);result.put("attractionScore",metrics.stream().mapToDouble(m->(Double)m.get("value")).average().stream().boxed().findFirst().orElse(null));result.put("message",message);result.put("source","출처: ⓒ한국관광공사");return result;
    }
    private static String first(JsonNode n,String[] keys){for(String k:keys){String v=n.path(k).asText("").trim();if(!v.isEmpty())return v;}return "";}
    private static String firstOr(JsonNode n,String fallback,String[] keys){String v=first(n,keys);return v.isEmpty()?fallback:v;}
    private static Double number(JsonNode n,String[] keys){for(String k:keys){String v=n.path(k).asText("").replace(",","");try{return Double.parseDouble(v);}catch(Exception ignored){}}return null;}
}
