package com.project.service;

import com.project.model.CaseState;

/**
 * Result returned after a case is paused in the frozen state.
 */
public record CaseFreezeResult(CaseState caseState) {
}
