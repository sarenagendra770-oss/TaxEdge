package com.taxedge.modules.notification.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.notification.dto.NotificationRequest;
import com.taxedge.modules.notification.entity.Notification;
import com.taxedge.modules.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repo;
    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    public Notification send(NotificationRequest r) {
        return repo.save(Notification.builder()
                .userId(r.getUserId()).title(r.getTitle()).message(r.getMessage())
                .type(r.getType() == null ? "INFO" : r.getType()).readFlag(false).build());
    }

    public List<Notification> listMine(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }
    public long unreadCount(Long userId) { return repo.countByUserIdAndReadFlagFalse(userId); }

    public Notification markRead(Long id, Long userId, boolean isAdmin) {
        Notification n = repo.findById(id).orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !n.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        n.setReadFlag(true);
        return repo.save(n);
    }

    public void delete(Long id, Long userId, boolean isAdmin) {
        Notification n = repo.findById(id).orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !n.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        repo.delete(n);
    }
}
