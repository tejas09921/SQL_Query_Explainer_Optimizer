package com.sqlexplainer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.Instant;

@Entity
public class QueryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private String sql;

    private double planningTimeMs;

    private double executionTimeMs;

    @Lob
    @Column(nullable = false)
    private String planJson;

    @Lob
    @Column(nullable = false)
    private String bottlenecksJson;

    @Lob
    @Column(nullable = false)
    private String suggestionsJson;

    @Lob
    @Column(nullable = false)
    private String explanation;

    @Column(nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public double getPlanningTimeMs() {
        return planningTimeMs;
    }

    public void setPlanningTimeMs(double planningTimeMs) {
        this.planningTimeMs = planningTimeMs;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(double executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getPlanJson() {
        return planJson;
    }

    public void setPlanJson(String planJson) {
        this.planJson = planJson;
    }

    public String getBottlenecksJson() {
        return bottlenecksJson;
    }

    public void setBottlenecksJson(String bottlenecksJson) {
        this.bottlenecksJson = bottlenecksJson;
    }

    public String getSuggestionsJson() {
        return suggestionsJson;
    }

    public void setSuggestionsJson(String suggestionsJson) {
        this.suggestionsJson = suggestionsJson;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
