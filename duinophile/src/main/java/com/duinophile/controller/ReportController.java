package com.duinophile.controller;

import com.duinophile.model.Report;
import com.duinophile.model.Comment;
import com.duinophile.service.ReportService;
import com.duinophile.service.CommentService;
import com.duinophile.web.CurrentUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private CommentService commentService;

    @PostMapping("/create")
    public String createReport(@RequestParam String commentId, @RequestParam String reason, @ModelAttribute("currentUser") CurrentUser user, RedirectAttributes redirectAttributes, @RequestHeader(value = "Referer", required = false) String referer) {
        if (user == null) return "redirect:/users/login";

        Optional<Comment> commentOpt = commentService.getCommentById(commentId);
        if (commentOpt.isPresent()) {
            Comment c = commentOpt.get();
            Report report = new Report();
            report.setReporterId(user.id());
            report.setReporterName(user.username());
            report.setCommentId(c.getId());
            report.setPostId(c.getPostId());
            report.setCommentContent(c.getContent());
            report.setCommentAuthorName(c.getAuthorName() != null ? c.getAuthorName() : "Scholar");
            report.setReason(reason);
            report.setStatus("PENDING");
            reportService.createReport(report);
            redirectAttributes.addFlashAttribute("success", "Comment successfully reported for review.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Comment no longer exists.");
        }
        return "redirect:" + (referer != null ? referer : "/posts/feed");
    }

    @GetMapping("/manage")
    public String manageReports(@ModelAttribute("currentUser") CurrentUser user, Model model) {
        if (user == null || (!"ADMIN".equals(user.role()) && !"STAFF".equals(user.role()))) {
            return "redirect:/";
        }
        model.addAttribute("reports", reportService.getPendingReports());
        model.addAttribute("view", "manage-reports");
        return "layout";
    }

    @PostMapping("/resolve/{id}")
    public String resolveReport(@PathVariable String id, @RequestParam String action, @ModelAttribute("currentUser") CurrentUser user, RedirectAttributes redirectAttributes) {
        if (user == null || (!"ADMIN".equals(user.role()) && !"STAFF".equals(user.role()))) {
            return "redirect:/";
        }
        
        Optional<Report> reportOpt = reportService.getReportById(id);
        if (reportOpt.isPresent()) {
            Report r = reportOpt.get();
            if ("ACCEPT".equalsIgnoreCase(action)) {
                commentService.deleteComment(r.getCommentId());
                reportService.updateReportStatus(id, "ACCEPTED");
                redirectAttributes.addFlashAttribute("success", "Report accepted and offending comment deleted.");
            } else if ("REJECT".equalsIgnoreCase(action)) {
                reportService.updateReportStatus(id, "REJECTED");
                redirectAttributes.addFlashAttribute("success", "Report rejected.");
            }
        }
        return "redirect:/reports/manage";
    }

    @GetMapping("/my-reports")
    public String myReports(@ModelAttribute("currentUser") CurrentUser user, Model model) {
        if (user == null) return "redirect:/users/login";
        model.addAttribute("reports", reportService.getReportsByUser(user.id()));
        model.addAttribute("view", "my-reports");
        return "layout";
    }
}
