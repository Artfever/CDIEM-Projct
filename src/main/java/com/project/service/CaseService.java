package com.project.service;

import com.project.model.Case;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;

public class CaseService {
    private final CaseRepository caseRepository;

    public CaseService() {
        this(new CaseRepositoryImpl());
    }

    public CaseService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    public void createCase(Case c) {
        caseRepository.save(c);
    }
}
