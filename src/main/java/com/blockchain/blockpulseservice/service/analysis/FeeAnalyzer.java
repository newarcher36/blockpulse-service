package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;

public interface FeeAnalyzer {
    AnalysisContext analyze(AnalysisContext context);
    FeeAnalyzer setNext(FeeAnalyzer next);
}