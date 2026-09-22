package com.trimly.api.repository;

import com.trimly.api.model.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    Optional<QrCode> findByShortCode(String shortCode);

    Optional<QrCode> findByShortCodeAndUserId(String shortCode, Long userId);

    List<QrCode> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByShortCode(String shortCode);

    void deleteByShortCode(String shortCode);
}
