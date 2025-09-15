package com.blockchain.blockpulseservice.config.analysis;

import com.blockchain.blockpulseservice.service.analysis.price_tier.PriceTierAnalyzer;
import com.blockchain.blockpulseservice.service.analysis.OutlierFeeAnalyzer;
import com.blockchain.blockpulseservice.service.analysis.SurgeFeeAnalyzer;
import com.blockchain.blockpulseservice.service.analysis.ScamFeeAnalyzer;
import com.blockchain.blockpulseservice.service.analysis.FeeAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalysisChainConfig {

    @Bean
    public FeeAnalyzer analysisChain(OutlierFeeAnalyzer outlierAnalyzer,
                                     ScamFeeAnalyzer scamFeeAnalyzer,
                                     SurgeFeeAnalyzer surgeFeeAnalyzer,
                                     PriceTierAnalyzer priceTierAnalyzer) {
        outlierAnalyzer
                .setNext(scamFeeAnalyzer)
                .setNext(surgeFeeAnalyzer)
                .setNext(priceTierAnalyzer);

        return outlierAnalyzer;
    }
}
