package com.duinophile.service;

import com.duinophile.model.Report;
import com.duinophile.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReportService {
    
    @Autowired
    private ReportRepository reportRepository;

    public Report createReport(Report report) {
        return reportRepository.save(report);
    }

    public List<Report> getPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    public List<Report> getReportsByUser(String userId) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Report> getReportById(String id) {
        return reportRepository.findById(id);
    }

    public Report updateReportStatus(String id, String status) {
        Optional<Report> opt = reportRepository.findById(id);
        if (opt.isPresent()) {
            Report r = opt.get();
            r.setStatus(status);
            return reportRepository.save(r);
        }
        return null;
    }
}
