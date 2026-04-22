package com.project.repository;

import com.project.model.Case;

public class CaseRepositoryImpl implements CaseRepository {
    @Override
    public void save(Case c) {
        System.out.println("Saving case: " + c.getTitle() + " [" + c.getSeverity() + "]");
    }
}
