package kr.dasibom.application;

import com.fasterxml.jackson.databind.JsonNode;
import kr.dasibom.infrastructure.TourApiClient;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VisitorInsightService {
    public record Snapshot(LocalDate asOf,Map<String,Long> visitors,Map<String,Integer> hiddenScores) {}
    private static final ZoneId KST=ZoneId.of("Asia/Seoul");
    private final TourApiClient api;
    public VisitorInsightService(TourApiClient api){this.api=api;}

    public Snapshot latest(){
        LocalDate start=LocalDate.now(KST).minusDays(30);
        for(int back=0;back<21;back++){
            LocalDate date=start.minusDays(back);String ymd=date.format(DateTimeFormatter.BASIC_ISO_DATE);
            List<JsonNode> rows=api.fetch("DataLabService/locgoRegnVisitrDDList",Map.of("startYmd",ymd,"endYmd",ymd),1000);
            if(rows.isEmpty())continue;
            Map<String,Long> sums=new HashMap<>();
            for(JsonNode row:rows){
                String code=text(row,"signguCode","signguCd");
                if(code.length()!=5)continue;
                sums.merge(code,longValue(row,"touNum","visitorNum"),Long::sum);
            }
            if(!sums.isEmpty())return new Snapshot(date,Map.copyOf(sums),scores(sums));
        }
        return new Snapshot(null,Map.of(),Map.of());
    }
    private static Map<String,Integer> scores(Map<String,Long> visitors){
        List<Long> ordered=visitors.values().stream().sorted().toList();Map<String,Integer> out=new HashMap<>();
        int denominator=Math.max(1,ordered.size()-1);
        visitors.forEach((code,count)->{
            int rank=Collections.binarySearch(ordered,count);if(rank<0)rank=0;
            out.put(code,(int)Math.round(100.0*(denominator-rank)/denominator));
        });
        return Map.copyOf(out);
    }
    private static String text(JsonNode n,String...keys){for(String k:keys){String v=n.path(k).asText("").trim();if(!v.isEmpty())return v;}return "";}
    private static long longValue(JsonNode n,String...keys){for(String k:keys){String v=n.path(k).asText("").replace(",","");try{return Math.round(Double.parseDouble(v));}catch(Exception ignored){}}return 0;}
}
