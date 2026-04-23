package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
    name = "url_daily_clicks",
    uniqueConstraints = @UniqueConstraint(name = "uk_url_daily_clicks_mapping_date", columnNames = {"url_mapping_id", "access_date"})
)
public class UrlDailyClick {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    private UrlMapping urlMapping;

    @Column(name = "access_date", nullable = false)
    private LocalDate accessDate;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    protected UrlDailyClick() {
    }

    public UrlDailyClick(UrlMapping urlMapping, LocalDate accessDate) {
        this.urlMapping = urlMapping;
        this.accessDate = accessDate;
        this.clickCount = 0;
    }

    public LocalDate getAccessDate() {
        return accessDate;
    }

    public long getClickCount() {
        return clickCount;
    }

    public void incrementClickCount() {
        clickCount++;
    }
}
