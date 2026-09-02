package com.taxedge.modules.tds.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.tds.dto.TdsRequest;
import com.taxedge.modules.tds.entity.TdsRecord;
import com.taxedge.modules.tds.repository.TdsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TdsService {

    private final TdsRepository repo;
    public TdsService(TdsRepository repo) { this.repo = repo; }

    public TdsRecord create(Long userId, TdsRequest r) {
        return repo.save(TdsRecord.builder()
                .userId(userId).tan(r.getTan()).deductorName(r.getDeductorName())
                .financialYear(r.getFinancialYear()).quarter(r.getQuarter())
                .tdsAmount(r.getTdsAmount()).section(r.getSection())
                .status(r.getStatus() == null ? "PENDING" : r.getStatus())
                .remarks(r.getRemarks()).build());
    }
    public List<TdsRecord> listMine(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }
    public List<TdsRecord> listAll() { return repo.findAll(); }
    public TdsRecord get(Long id, Long userId, boolean isAdmin) {
        TdsRecord e = repo.findById(id).orElseThrow(() -> new ApiException("TDS record not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !e.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        return e;
    }
    public TdsRecord update(Long id, Long userId, boolean isAdmin, TdsRequest r) {
        TdsRecord e = get(id, userId, isAdmin);
        e.setTan(r.getTan()); e.setDeductorName(r.getDeductorName());
        e.setFinancialYear(r.getFinancialYear()); e.setQuarter(r.getQuarter());
        e.setTdsAmount(r.getTdsAmount()); e.setSection(r.getSection());
        if (r.getStatus() != null) e.setStatus(r.getStatus());
        e.setRemarks(r.getRemarks());
        return repo.save(e);
    }
    public void delete(Long id, Long userId, boolean isAdmin) { repo.delete(get(id, userId, isAdmin)); }
}
