package com.taxedge.modules.payment.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.payment.dto.PaymentRequest;
import com.taxedge.modules.payment.entity.Payment;
import com.taxedge.modules.payment.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository repo;
    public PaymentService(PaymentRepository repo) { this.repo = repo; }

    public Payment initiate(Long userId, PaymentRequest r) {
        return repo.save(Payment.builder()
                .userId(userId).amount(r.getAmount())
                .currency(r.getCurrency() == null ? "INR" : r.getCurrency())
                .purpose(r.getPurpose())
                .provider(r.getProvider() == null ? "STUB" : r.getProvider())
                .transactionId("TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .status("PENDING").remarks(r.getRemarks()).build());
    }

    public Payment markStatus(String transactionId, String status) {
        Payment p = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new ApiException("Transaction not found", HttpStatus.NOT_FOUND));
        p.setStatus(status);
        return repo.save(p);
    }

    public List<Payment> listMine(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }
    public List<Payment> listAll() { return repo.findAll(); }

    public Payment get(Long id, Long userId, boolean isAdmin) {
        Payment p = repo.findById(id).orElseThrow(() -> new ApiException("Payment not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !p.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        return p;
    }
}
