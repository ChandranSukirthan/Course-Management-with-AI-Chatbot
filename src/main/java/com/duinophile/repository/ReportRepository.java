package com.duinophile.repository;

import com.duinophile.model.Report;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReportRepository extends MongoRepository<Report, String> {
    List<Report> findByStatusOrderByCreatedAtDesc(String status);
    List<Report> findByReporterIdOrderByCreatedAtDesc(String reporterId);
    List<Report> findAllByOrderByCreatedAtDesc();
}
