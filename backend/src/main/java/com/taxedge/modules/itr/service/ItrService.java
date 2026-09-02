package com.taxedge.modules.itr.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.itr.dto.ItrReturnRequest;
import com.taxedge.modules.itr.entity.ItrReturn;
import com.taxedge.modules.itr.repository.ItrReturnRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItrService {

    private final ItrReturnRepository repo;

    public ItrService(ItrReturnRepository repo) { this.repo = repo; }

    public ItrReturn create(Long userId, ItrReturnRequest r) {
        return repo.save(ItrReturn.builder()
                .userId(userId).pan(r.getPan()).assessmentYear(r.getAssessmentYear()).itrForm(r.getItrForm())
                .totalIncome(r.getTotalIncome()).taxLiability(r.getTaxLiability()).refundAmount(r.getRefundAmount())
                .status(r.getStatus() == null ? "DRAFT" : r.getStatus())
                .filedDate(r.getFiledDate()).remarks(r.getRemarks()).build());
    }

    public List<ItrReturn> listMine(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }
    public List<ItrReturn> listAll() { return repo.findAll(); }

    public ItrReturn get(Long id, Long userId, boolean isAdmin) {
        ItrReturn e = repo.findById(id).orElseThrow(() -> new ApiException("ITR not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !e.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        return e;
    }

    public ItrReturn update(Long id, Long userId, boolean isAdmin, ItrReturnRequest r) {
        ItrReturn e = get(id, userId, isAdmin);
        e.setPan(r.getPan()); e.setAssessmentYear(r.getAssessmentYear()); e.setItrForm(r.getItrForm());
        e.setTotalIncome(r.getTotalIncome()); e.setTaxLiability(r.getTaxLiability()); e.setRefundAmount(r.getRefundAmount());
        if (r.getStatus() != null) e.setStatus(r.getStatus());
        e.setFiledDate(r.getFiledDate()); e.setRemarks(r.getRemarks());
        return repo.save(e);
    }

    public void delete(Long id, Long userId, boolean isAdmin) { repo.delete(get(id, userId, isAdmin)); }
}
