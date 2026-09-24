package kr.dasibom.application;

import com.fasterxml.jackson.databind.JsonNode;
import kr.dasibom.domain.Region;
import kr.dasibom.infrastructure.TourApiClient;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Service
public class MetricService {
    private static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("1101","레포츠 SNS 언급량"),Map.entry("1102","휴식·힐링 SNS 언급량"),Map.entry("1103","미식 SNS 언급량"),Map.entry("1104","체험 SNS 언급량"),
        Map.entry("1105","쇼핑업 소비액"),Map.entry("1106","식음료 소비액"),Map.entry("1107","숙박업 소비액"),Map.entry("1108","여가 서비스업 소비액"),
        Map.entry("1109","운송업 소비액"),Map.entry("1110","숙박 검색량"),Map.entry("1111","음식 검색량"),Map.entry("1112","쇼핑 검색량"),
        Map.entry("1201","문화관광 검색량"),Map.entry("1202","레저스포츠 검색량"),Map.entry("1203","역사관광 검색량"),Map.entry("1204","체험관광 검색량"),Map.entry("1205","자연관광 검색량")
    );
    private static final Map<String,String> THEMES=Map.ofEntries(
        Map.entry("1205","NATURE"),Map.entry("1102","HEALING"),Map.entry("1103","FOOD"),Map.entry("1106","FOOD"),Map.entry("1111","FOOD"),
        Map.entry("1201","CULTURE"),Map.entry("1203","CULTURE"),Map.entry("1107","STAY"),Map.entry("1110","STAY"),
        Map.entry("1101","ACTIVITY"),Map.entry("1104","ACTIVITY"),Map.entry("1108","ACTIVITY"),Map.entry("1202","ACTIVITY"),Map.entry("1204","ACTIVITY")
    );
    private final TourApiClient api;
    public MetricService(TourApiClient api){this.api=api;}
    public Map<String,Object> metrics(Region region){
        YearMonth month=YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1);
        String availableMonth=null,availableCode=null;
        // Although documented as optional, the live API returns no rows without an index code.
        // First find an available month with the aggregate index, then request the 17 real indices.
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        try {
            for(int i=0;i<12&&System.nanoTime()<deadline;i++) {
                String baseYm=month.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM"));
                String code=baseYm.compareTo("202607")>=0?RegionCodes.currentCode(region.getCode()):region.getCode();
                var rows=api.fetch("AreaTarResDemService/areaTarSvcDemList",Map.of("baseYm",baseYm,"areaCd",code.substring(0,2),"signguCd",code,"tarSvcDemIxCd","11"),1);
                if(!rows.isEmpty()){availableMonth=baseYm;availableCode=code;break;}
            }
        }catch(TourApiClient.TourApiException e){return result("ERROR",null,List.of(),"지표 연결이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.");}
        if(availableMonth==null)return result("UNAVAILABLE",null,List.of(),"조회한 기간에 제공된 자원 수요 지표가 없습니다. 사진과 여행 장소는 계속 둘러볼 수 있어요.");
        final String baseYm=availableMonth,regionCode=availableCode;
        List<Callable<List<Map<String,Object>>>> tasks=new ArrayList<>();
        for(String index:LABELS.keySet().stream().sorted().toList())tasks.add(()->{
            boolean culture=index.startsWith("12");
            var params=Map.of("baseYm",baseYm,"areaCd",regionCode.substring(0,2),"signguCd",regionCode,culture?"culResDemIxCd":"tarSvcDemIxCd",index);
            return parseMetrics(api.fetch("AreaTarResDemService/"+(culture?"areaCulResDemList":"areaTarSvcDemList"),params,1),culture);
        });
        List<Map<String,Object>> items=new ArrayList<>();boolean failed=false;
        try(var executor=Executors.newFixedThreadPool(6)) {
            for(var future:executor.invokeAll(tasks,9,TimeUnit.SECONDS))try{items.addAll(future.get());}catch(Exception e){failed=true;}
        }catch(InterruptedException e){Thread.currentThread().interrupt();failed=true;}
        if(!items.isEmpty())return result("LIVE",baseYm,items,failed?"일부 지표는 연결되지 않아 확인된 값만 표시합니다.":null);
        return result(failed?"ERROR":"UNAVAILABLE",baseYm,List.of(),failed?"지표를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.":"이 지역의 세부 지표가 아직 제공되지 않습니다.");
    }
    public static List<Map<String,Object>> parseMetrics(List<JsonNode> rows,boolean culture){
        Map<String,Map<String,Object>> out=new TreeMap<>();
        String prefix=culture?"culResDemIx":"tarSvcDemIx";
        for(JsonNode row:rows){
            String code=row.path(prefix+"Cd").asText("");
            if(!LABELS.containsKey(code))continue;
            double value;
            try{value=Double.parseDouble(row.path(prefix+"Val").asText().replace(",",""));}catch(NumberFormatException e){continue;}
            if(!Double.isFinite(value)||value<0||value>100)continue;
            out.put(code,Map.of("code",code,"name",row.path(prefix+"Nm").asText(LABELS.get(code)),"theme",THEMES.getOrDefault(code,"OTHER"),"value",value));
        }
        return List.copyOf(out.values());
    }
    private static Map<String,Object> result(String status,String baseYm,List<Map<String,Object>> metrics,String message){
        Map<String,List<Double>> groups=new LinkedHashMap<>();for(String theme:List.of("NATURE","HEALING","FOOD","CULTURE","STAY","ACTIVITY"))groups.put(theme,new ArrayList<>());
        metrics.forEach(m->{var group=groups.get(String.valueOf(m.get("theme")));if(group!=null)group.add((Double)m.get("value"));});
        List<Map<String,Object>> radar=new ArrayList<>();
        groups.forEach((theme,values)->{Map<String,Object> point=new LinkedHashMap<>();point.put("theme",theme);point.put("value",values.isEmpty()?null:values.stream().mapToDouble(Double::doubleValue).average().orElseThrow());radar.add(point);});
        Map<String,Object> result=new LinkedHashMap<>();result.put("status",status);result.put("baseYm",baseYm);result.put("items",metrics);result.put("radar",radar);result.put("attractionScore",metrics.stream().mapToDouble(m->(Double)m.get("value")).average().stream().boxed().findFirst().orElse(null));result.put("message",message);result.put("source","출처: ⓒ한국관광공사");return result;
    }
}
