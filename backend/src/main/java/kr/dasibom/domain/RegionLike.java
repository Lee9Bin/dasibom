package kr.dasibom.domain;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
@Entity @Table(name="region_likes")
public class RegionLike {
    @Embeddable public static class Id implements Serializable {
        private String regionCode;private String visitorId;
        public Id(){}public Id(String code,String visitor){regionCode=code;visitorId=visitor;}
        @Override public boolean equals(Object other){return other instanceof Id id&&Objects.equals(regionCode,id.regionCode)&&Objects.equals(visitorId,id.visitorId);}
        @Override public int hashCode(){return Objects.hash(regionCode,visitorId);}
    }
    @EmbeddedId private Id id;
    private Instant createdAt;
    protected RegionLike(){}
}
