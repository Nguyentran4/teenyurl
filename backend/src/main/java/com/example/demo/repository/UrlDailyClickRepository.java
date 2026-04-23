package com.example.demo.repository;

import com.example.demo.model.UrlDailyClick;
import com.example.demo.model.UrlMapping;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlDailyClickRepository extends JpaRepository<UrlDailyClick, Long> {
    Optional<UrlDailyClick> findByUrlMappingAndAccessDate(UrlMapping urlMapping, LocalDate accessDate);

    List<UrlDailyClick> findByUrlMappingOrderByAccessDateAsc(UrlMapping urlMapping);
}
