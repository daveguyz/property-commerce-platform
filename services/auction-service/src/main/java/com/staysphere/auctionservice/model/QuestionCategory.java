package com.staysphere.auctionservice.model;

public enum QuestionCategory {
    PROPERTY_INFO,   // questions about the physical property
    BIDDING_RULES,   // questions about auction rules / process
    TECHNICAL,       // platform or streaming issues
    GENERAL,         // catch-all
    COMPLAINT        // conduct issues — eligible for escalation
}
