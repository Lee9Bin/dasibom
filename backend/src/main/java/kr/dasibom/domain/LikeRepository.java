package kr.dasibom.domain;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
public interface LikeRepository extends JpaRepository<RegionLike,RegionLike.Id> {
    long countByIdRegionCode(String code);
    @Modifying @Transactional
    @Query(value="INSERT IGNORE INTO region_likes(region_code,visitor_id) VALUES(:code,:visitor)",nativeQuery=true)
    void like(@Param("code") String code,@Param("visitor") String visitor);
    @Modifying @Transactional
    @Query("DELETE FROM RegionLike l WHERE l.id.regionCode=:code AND l.id.visitorId=:visitor")
    void unlike(@Param("code") String code,@Param("visitor") String visitor);
}
