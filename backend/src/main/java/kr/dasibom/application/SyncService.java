package kr.dasibom.application;

import kr.dasibom.domain.*;
import kr.dasibom.infrastructure.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SyncService {
    private final TourApiClient api;
    private final DatasetStore store;
    private final CatalogService catalog;
    private final SyncRunRepository runs;
    private final boolean enabled;
    private final AtomicBoolean running=new AtomicBoolean(false);
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    public SyncService(TourApiClient api,DatasetStore store,CatalogService catalog,SyncRunRepository runs,@Value("${dasibom.sync-enabled}") boolean enabled){this.api=api;this.store=store;this.catalog=catalog;this.runs=runs;this.enabled=enabled;}
    public boolean start() {
        if(!api.configured())throw new IllegalStateException("TourAPI 인증키를 설정해 주세요.");
        if(!running.compareAndSet(false,true))return false;
        executor.submit(this::run);return true;
    }
    @Scheduled(cron="0 40 3 * * *",zone="Asia/Seoul")
    void scheduled(){if(enabled)start();}
    private void run(){
        int success=0, failed=0;List<String> errors=new ArrayList<>(); SyncRun run=null;
        try{
            run=runs.save(new SyncRun());
            for(Region r:catalog.all()){
                for(String kind:List.of("photos","places","crowding")){
                    try{
                        var data=switch(kind){
                            case "photos" -> api.fetch("PhotoGalleryService1/gallerySearchList1",Map.of("keyword",r.getName(),"arrange","C"),48).stream().filter(photo->CatalogService.matchesPhotoRegion(r.getName(),photo)).limit(36).toList();
                            case "places" -> api.fetch("KorService2/areaBasedList2",Map.of("lDongRegnCd",r.getAreaCode(),"lDongSignguCd",r.getCode().substring(2),"arrange","Q","contentTypeId","12"),12);
                            default -> api.fetch("TatsCnctrRateService/tatsCnctrRatedList",Map.of("areaCd",r.getAreaCode(),"signguCd",r.getCode(),"tAtsNm",r.getAnchorPlace()),30);
                        };
                        if(data.isEmpty() && !store.read(r.getCode(),kind).isEmpty()) {
                            failed++;errors.add(r.getCode()+"/"+kind+":EMPTY_RESPONSE_RETAINED");
                        } else {store.save(r.getCode(),kind,data);success++;}
                    }catch(TourApiClient.TourApiException e){failed++;errors.add(r.getCode()+"/"+kind+":"+e.getMessage());}
                    Thread.sleep(150);
                }
            }
            String summary=String.join(";",errors);
            run.finish(failed==0?"SUCCESS":"PARTIAL",success,failed,summary.substring(0,Math.min(summary.length(),1900)));runs.save(run);
        }catch(Exception e){
            if(e instanceof InterruptedException)Thread.currentThread().interrupt();
            if(run!=null){run.finish("FAILED",success,failed,"수집 작업 실패. 마지막 정상 데이터 유지.");runs.save(run);}
        }finally{running.set(false);}
    }
    public boolean running(){return running.get();}
    public List<SyncRun> history(){return runs.findTop20ByOrderByIdDesc();}
    @PreDestroy void close(){executor.shutdownNow();}
}
