package com.propertycommerce.auctionservice.model;

public enum KycStatus {
    NOT_STARTED,     // no KYC session created yet
    SESSION_CREATED, // Stripe Identity session created, pending user action
    PROCESSING,      // user submitted documents, Stripe processing
    VERIFIED,        // KYC passed — bidder cleared for high-value lots
    FAILED,          // documents rejected or identity mismatch
    REQUIRES_INPUT,  // additional documents requested by Stripe
    CANCELLED        // session cancelled before completion
}
