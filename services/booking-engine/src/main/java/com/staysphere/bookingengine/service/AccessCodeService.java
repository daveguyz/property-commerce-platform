package com.staysphere.bookingengine.service;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class AccessCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public String generateAccessCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) code.append('-');
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return code.toString();
    }
}
