package com.propertycommerce.bookingengine.model;

public enum AgreementStatus {
    DRAFT,            // generated, not yet sent to parties
    SENT,             // emailed to buyer and seller for signature
    BUYER_SIGNED,     // buyer has signed
    SELLER_SIGNED,    // seller has signed (buyer not yet)
    FULLY_EXECUTED,   // both parties signed — binding
    DEFAULTED,        // buyer failed to pay by deadline
    CANCELLED         // withdrawn before full execution
}
