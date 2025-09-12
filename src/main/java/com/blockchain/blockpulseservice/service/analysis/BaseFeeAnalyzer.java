package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;

public abstract class BaseFeeAnalyzer implements FeeAnalyzer {
    private FeeAnalyzer next;

    @Override
    public FeeAnalyzer setNext(FeeAnalyzer next) {
        this.next = next;
        return next;
    }

    @Override
    public final AnalysisContext analyze(AnalysisContext context) {
        var updatedContext = doAnalyze(context);
        
        if (next != null) {
            return next.analyze(updatedContext);
        }
        return updatedContext;
    }

    protected abstract AnalysisContext doAnalyze(AnalysisContext context);
}