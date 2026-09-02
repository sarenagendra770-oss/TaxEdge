package com.taxedge.modules.insurance.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.insurance.dto.PolicyRequest;
import com.taxedge.modules.insurance.entity.Policy;
import com.taxedge.modules.insurance.repository.PolicyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InsuranceService {

    private final PolicyRepository repo;
    public InsuranceService(PolicyRepository repo) { this.repo = repo; }

    public Policy create(Long userId, PolicyRequest r) {
        return repo.save(Policy.builder()
                .userId(userId).provider(r.getProvider()).policyType(r.getPolicyType())
                .policyNumber(r.getPolicyNumber()).sumAssured(r.getSumAssured()).premium(r.getPremium())
                .startDate(r.getStartDate()).endDate(r.getEndDate())
                .status(r.getStatus() == null ? "ACTIVE" : r.getStatus())
                .remarks(r.getRemarks()).build());
    }
    public List<Policy> listMine(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }
    public List<Policy> listAll() { return repo.findAll(); }
    public Policy get(Long id, Long userId, boolean isAdmin) {
        Policy e = repo.findById(id).orElseThrow(() -> new ApiException("Policy not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !e.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        return e;
    }
    public Policy update(Long id, Long userId, boolean isAdmin, PolicyRequest r) {
        Policy e = get(id, userId, isAdmin);
        e.setProvider(r.getProvider()); e.setPolicyType(r.getPolicyType()); e.setPolicyNumber(r.getPolicyNumber());
        e.setSumAssured(r.getSumAssured()); e.setPremium(r.getPremium());
        e.setStartDate(r.getStartDate()); e.setEndDate(r.getEndDate());
        if (r.getStatus() != null) e.setStatus(r.getStatus());
        e.setRemarks(r.getRemarks());
        return repo.save(e);
    }
    public void delete(Long id, Long userId, boolean isAdmin) { repo.delete(get(id, userId, isAdmin)); }
}
