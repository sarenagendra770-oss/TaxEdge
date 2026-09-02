package com.taxedge.modules.loan.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.loan.dto.LoanRequest;
import com.taxedge.modules.loan.entity.LoanApplication;
import com.taxedge.modules.loan.repository.LoanApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class LoanService {

    private final LoanApplicationRepository repo;
    public LoanService(LoanApplicationRepository repo) { this.repo = repo; }

    private BigDecimal computeEmi(BigDecimal p, BigDecimal annualRate, int months) {
        if (p == null || annualRate == null || months <= 0) return null;
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        if (r.signum() == 0) return p.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        double rd = r.doubleValue();
        double pow = Math.pow(1 + rd, months);
        double emi = p.doubleValue() * rd * pow / (pow - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    public LoanApplication create(Long userId, LoanRequest r) {
        BigDecimal emi = r.getEmi() != null ? r.getEmi() : computeEmi(r.getAmount(), r.getInterestRate(), r.getTenureMonths());
        return repo.save(LoanApplication.builder()
                .userId(userId).loanType(r.getLoanType()).amount(r.getAmount())
                .tenureMonths(r.getTenureMonths()).interestRate(r.getInterestRate())
                .emi(emi).purpose(r.getPurpose())
                .status(r.getStatus() == null ? "PENDING" : r.getStatus())
                .remarks(r.getRemarks()).build());
    }
    public List<LoanApplication> listMine(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }
    public List<LoanApplication> listAll() { return repo.findAll(); }
    public LoanApplication get(Long id, Long userId, boolean isAdmin) {
        LoanApplication e = repo.findById(id).orElseThrow(() -> new ApiException("Loan not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !e.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        return e;
    }
    public LoanApplication update(Long id, Long userId, boolean isAdmin, LoanRequest r) {
        LoanApplication e = get(id, userId, isAdmin);
        e.setLoanType(r.getLoanType()); e.setAmount(r.getAmount()); e.setTenureMonths(r.getTenureMonths());
        e.setInterestRate(r.getInterestRate());
        e.setEmi(r.getEmi() != null ? r.getEmi() : computeEmi(r.getAmount(), r.getInterestRate(), r.getTenureMonths()));
        e.setPurpose(r.getPurpose());
        if (r.getStatus() != null) e.setStatus(r.getStatus());
        e.setRemarks(r.getRemarks());
        return repo.save(e);
    }
    public LoanApplication updateStatus(Long id, String status) {
        LoanApplication e = repo.findById(id).orElseThrow(() -> new ApiException("Loan not found", HttpStatus.NOT_FOUND));
        e.setStatus(status);
        return repo.save(e);
    }
    public void delete(Long id, Long userId, boolean isAdmin) { repo.delete(get(id, userId, isAdmin)); }
}
