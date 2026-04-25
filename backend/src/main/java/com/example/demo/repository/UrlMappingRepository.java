package com.example.demo.repository;

import com.example.demo.model.UrlMapping;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Query("select coalesce(sum(mapping.clickCount), 0) from UrlMapping mapping")
    long sumClickCount();

    @Modifying
    @Query("""
        update UrlMapping mapping
        set mapping.clickCount = mapping.clickCount + 1
          , mapping.lastAccessedAt = :accessedAt
          , mapping.lastUserAgent = :userAgent
          , mapping.lastReferrer = :referrer
          , mapping.lastIpHash = :ipHash
        where mapping.shortCode = :shortCode
          and mapping.active = true
          and (mapping.expiresAt is null or mapping.expiresAt > :now)
        """)
    int recordAccessForRedirect(
        @Param("shortCode") String shortCode,
        @Param("now") LocalDateTime now,
        @Param("accessedAt") LocalDateTime accessedAt,
        @Param("userAgent") String userAgent,
        @Param("referrer") String referrer,
        @Param("ipHash") String ipHash
    );
}
