package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;

public abstract class BaseTransactionAnalyzer implements TransactionAnalyzer {
    private TransactionAnalyzer next;

    @Override
    public TransactionAnalyzer setNext(TransactionAnalyzer next) {
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