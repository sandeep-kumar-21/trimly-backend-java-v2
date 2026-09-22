package com.trimly.api.repository;

import com.trimly.api.model.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Campaign> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query(value = "SELECT DISTINCT unnest(channels) FROM campaigns WHERE user_id = :userId AND channels IS NOT NULL", nativeQuery = true)
    List<String> findDistinctChannelsByUserId(@Param("userId") Long userId);
}
