package kr.dasibom.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="sync_runs")
public class SyncRun {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Instant startedAt=Instant.now();private Instant finishedAt;
    private String status="RUNNING";private int successful;private int failed;private String summary;
    public SyncRun(){}
    public void finish(String status,int successful,int failed,String summary){this.status=status;this.successful=successful;this.failed=failed;this.summary=summary;this.finishedAt=Instant.now();}
    public Long getId(){return id;}public Instant getStartedAt(){return startedAt;}public Instant getFinishedAt(){return finishedAt;}public String getStatus(){return status;}public int getSuccessful(){return successful;}public int getFailed(){return failed;}public String getSummary(){return summary;}
}
