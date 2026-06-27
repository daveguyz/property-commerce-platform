package com.staysphere.auctionservice.model;

public enum QuestionStatus {
    PENDING,    // awaiting auctioneer response
    ANSWERED,   // response provided
    DISMISSED,  // auctioneer dismissed without answer (bidder not notified)
    ESCALATED   // sent to platform support as a SupportTicket
}
