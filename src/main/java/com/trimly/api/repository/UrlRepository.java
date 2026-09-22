package com.trimly.api.repository;

import com.trimly.api.model.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long>, JpaSpecificationExecutor<Url> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Url> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Url> findByUserIdAndIsHiddenFalseOrderByCreatedAtDesc(Long userId);

    List<Url> findByCampaignId(Long campaignId);

    long countByUserId(Long userId);

    long countByCampaignId(Long campaignId);

    @Query(value = "SELECT nextval('url_code_seq')", nativeQuery = true)
    Long getNextUrlSequenceValue();

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode);

    @Query(value = "SELECT DISTINCT unnest(tags) FROM urls WHERE user_id = :userId AND tags IS NOT NULL", nativeQuery = true)
    List<String> findDistinctTagsByUserId(@Param("userId") Long userId);
}
