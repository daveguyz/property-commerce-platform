package com.staysphere.auctionservice.model;

public enum QuestionPriority {
    NORMAL,
    HIGH,
    URGENT  // auctioneer can mark URGENT; auto-set for COMPLAINT escalations
}
