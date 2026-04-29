package com.duinophile.service;

import com.duinophile.model.Feedback;
import com.duinophile.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.duinophile.web.CurrentUser;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback createFeedback(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }

    public List<Feedback> getFeedbackByCourseId(String courseId, CurrentUser currentUser) {
        List<Feedback> allFeedback = feedbackRepository.findByCourseId(courseId);
        if (currentUser != null && ("ADMIN".equals(currentUser.role()) || "STAFF".equals(currentUser.role()))) {
            return allFeedback;
        }
        return allFeedback.stream()
                .filter(fb -> "APPROVED".equals(fb.getStatus()) || 
                             (currentUser != null && currentUser.id().equals(fb.getUserId())))
                .collect(Collectors.toList());
    }

    public Optional<Feedback> getFeedbackById(String id) {
        return feedbackRepository.findById(id);
    }

    public Feedback updateFeedback(String id, Feedback feedbackDetails) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedback.setContent(feedbackDetails.getContent());
        feedback.setRating(feedbackDetails.getRating());
        feedback.setStatus("PENDING");
        return feedbackRepository.save(feedback);
    }

    public void approveFeedback(String id) {
        Feedback feedback = feedbackRepository.findById(id).orElse(null);
        if (feedback != null) {
            feedback.setStatus("APPROVED");
            feedbackRepository.save(feedback);
        }
    }

    public void deleteFeedback(String id) {
        feedbackRepository.deleteById(id);
    }
}
