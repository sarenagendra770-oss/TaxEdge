package com.taxedge.modules.gst.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.gst.dto.GstReturnRequest;
import com.taxedge.modules.gst.entity.GstReturn;
import com.taxedge.modules.gst.repository.GstReturnRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GstService {

    private final GstReturnRepository repo;

    public GstService(GstReturnRepository repo) {
        this.repo = repo;
    }

    public GstReturn create(Long userId, GstReturnRequest r) {
        GstReturn g = GstReturn.builder()
                .userId(userId)
                .gstin(r.getGstin())
                .periodMonth(r.getPeriodMonth())
                .periodYear(r.getPeriodYear())
                .returnType(r.getReturnType())
                .totalTaxableValue(r.getTotalTaxableValue())
                .totalTax(r.getTotalTax())
                .status(r.getStatus() == null ? "DRAFT" : r.getStatus())
                .filedDate(r.getFiledDate())
                .remarks(r.getRemarks())
                .build();
        return repo.save(g);
    }

    public List<GstReturn> listMine(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<GstReturn> listAll() {
        return repo.findAll();
    }

    public GstReturn get(Long id, Long userId, boolean isAdmin) {
        GstReturn g = repo.findById(id)
                .orElseThrow(() -> new ApiException("GST return not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !g.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        return g;
    }

    public GstReturn update(Long id, Long userId, boolean isAdmin, GstReturnRequest r) {
        GstReturn g = get(id, userId, isAdmin);
        g.setGstin(r.getGstin());
        g.setPeriodMonth(r.getPeriodMonth());
        g.setPeriodYear(r.getPeriodYear());
        g.setReturnType(r.getReturnType());
        g.setTotalTaxableValue(r.getTotalTaxableValue());
        g.setTotalTax(r.getTotalTax());
        if (r.getStatus() != null) g.setStatus(r.getStatus());
        g.setFiledDate(r.getFiledDate());
        g.setRemarks(r.getRemarks());
        return repo.save(g);
    }

    public void delete(Long id, Long userId, boolean isAdmin) {
        GstReturn g = get(id, userId, isAdmin);
        repo.delete(g);
    }
}
