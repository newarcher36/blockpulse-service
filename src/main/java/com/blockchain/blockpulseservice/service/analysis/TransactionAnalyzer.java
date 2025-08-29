package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;

public interface TransactionAnalyzer {
    AnalysisContext analyze(AnalysisContext context);
    TransactionAnalyzer setNext(TransactionAnalyzer next);
}