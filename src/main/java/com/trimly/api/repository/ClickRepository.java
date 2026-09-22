package com.trimly.api.repository;

import com.trimly.api.model.entity.Click;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long>, JpaSpecificationExecutor<Click> {

    List<Click> findTop20ByUserIdOrderByTimestampDesc(Long userId);

    List<Click> findByShortCodeOrderByTimestampDesc(String shortCode);

    Page<Click> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndTimestampBetween(Long userId, Instant start, Instant end);

    long countByUserIdAndIsQrScanTrue(Long userId);

    long countByUserIdAndIsQrScanTrueAndTimestampBetween(Long userId, Instant start, Instant end);

    long countByCampaignId(Long campaignId);

    long countByShortCode(String shortCode);

    @Query("SELECT COUNT(DISTINCT c.ipHash) FROM Click c WHERE c.userId = :userId")
    long countDistinctVisitorsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT c.ipHash) FROM Click c WHERE c.userId = :userId AND c.timestamp BETWEEN :start AND :end")
    long countDistinctVisitorsByUserIdAndTimestampBetween(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query(value = "SELECT country AS name, COUNT(*) AS count FROM clicks WHERE user_id = :userId AND country IS NOT NULL AND country != 'Unknown' GROUP BY country ORDER BY count DESC LIMIT 10", nativeQuery = true)
    List<Object[]> getTopCountries(@Param("userId") Long userId);

    @Query(value = "SELECT city AS city, country AS country, COUNT(*) AS count FROM clicks WHERE user_id = :userId AND city IS NOT NULL GROUP BY city, country ORDER BY count DESC LIMIT 10", nativeQuery = true)
    List<Object[]> getTopCities(@Param("userId") Long userId);

    @Query(value = "SELECT referrer AS name, COUNT(*) AS count FROM clicks WHERE user_id = :userId AND referrer IS NOT NULL AND referrer != '' GROUP BY referrer ORDER BY count DESC LIMIT 10", nativeQuery = true)
    List<Object[]> getTopReferrers(@Param("userId") Long userId);

    @Query(value = "SELECT device_type AS name, COUNT(*) AS count FROM clicks WHERE user_id = :userId GROUP BY device_type ORDER BY count DESC", nativeQuery = true)
    List<Object[]> getTopDevices(@Param("userId") Long userId);

    @Query(value = "SELECT browser AS name, COUNT(*) AS count FROM clicks WHERE user_id = :userId GROUP BY browser ORDER BY count DESC", nativeQuery = true)
    List<Object[]> getTopBrowsers(@Param("userId") Long userId);

    @Query(value = "SELECT os AS name, COUNT(*) AS count FROM clicks WHERE user_id = :userId GROUP BY os ORDER BY count DESC", nativeQuery = true)
    List<Object[]> getTopOs(@Param("userId") Long userId);

    @Query(value = "SELECT TO_CHAR(timestamp, 'YYYY-MM-DD') AS day, COUNT(*) AS count, COUNT(DISTINCT ip_hash) AS uniques " +
            "FROM clicks WHERE user_id = :userId AND timestamp >= :since " +
            "GROUP BY TO_CHAR(timestamp, 'YYYY-MM-DD') ORDER BY day ASC", nativeQuery = true)
    List<Object[]> getTimeSeriesDaily(@Param("userId") Long userId, @Param("since") Instant since);

    @Query(value = "SELECT utm_source AS name, COUNT(*) AS count FROM clicks WHERE user_id = :userId AND utm_source IS NOT NULL AND utm_source != '' GROUP BY utm_source ORDER BY count DESC LIMIT 10", nativeQuery = true)
    List<Object[]> getTopUtmSources(@Param("userId") Long userId);

    @Query(value = "SELECT utm_medium AS name, COUNT(*) AS count FROM clicks WHERE user_id = :userId AND utm_medium IS NOT NULL AND utm_medium != '' GROUP BY utm_medium ORDER BY count DESC LIMIT 10", nativeQuery = true)
    List<Object[]> getTopUtmMediums(@Param("userId") Long userId);

    @Query(value = "SELECT utm_campaign AS name, COUNT(*) AS count FROM clicks WHERE user_id = :userId AND utm_campaign IS NOT NULL AND utm_campaign != '' GROUP BY utm_campaign ORDER BY count DESC LIMIT 10", nativeQuery = true)
    List<Object[]> getTopUtmCampaigns(@Param("userId") Long userId);
}
