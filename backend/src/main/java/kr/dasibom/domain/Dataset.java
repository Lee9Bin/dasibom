package kr.dasibom.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity @Table(name="datasets")
public class Dataset {
    @Embeddable public static class Id implements Serializable {
        @Column(name="region_code") private String regionCode;
        private String kind;
        public Id(){} public Id(String regionCode,String kind){this.regionCode=regionCode;this.kind=kind;}
        public String getRegionCode(){return regionCode;}public String getKind(){return kind;}
        @Override public boolean equals(Object other){return other instanceof Id id&&Objects.equals(regionCode,id.regionCode)&&Objects.equals(kind,id.kind);}
        @Override public int hashCode(){return Objects.hash(regionCode,kind);}
    }
    @EmbeddedId private Id id;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="json") private String payload;
    private Instant fetchedAt;
    protected Dataset(){}
    public Dataset(String code,String kind,String payload){this.id=new Id(code,kind);this.payload=payload;this.fetchedAt=Instant.now();}
    public Id getId(){return id;}public String getPayload(){return payload;}public Instant getFetchedAt(){return fetchedAt;}
}
