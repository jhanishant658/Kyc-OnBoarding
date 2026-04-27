package com.kyc.config;

import com.kyc.entity.KycSubmission;
import com.kyc.entity.KycSubmission.KycStatus;
import com.kyc.entity.User;
import com.kyc.entity.User.Role;
import com.kyc.repository.KycSubmissionRepository;
import com.kyc.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds the database with sample data on every startup.
 * This runs automatically after Spring Boot initializes.
 *
 * Test credentials created here:
 *   merchant1 / password123 (MERCHANT)
 *   merchant2 / password123 (MERCHANT)
 *   reviewer1 / password123 (REVIEWER)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final KycSubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      KycSubmissionRepository submissionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Create users
        User merchant1 = createUser("merchant1", "password123", Role.MERCHANT);
        User merchant2 = createUser("merchant2", "password123", Role.MERCHANT);
        createUser("reviewer1", "password123", Role.REVIEWER);

        // Create sample submissions for merchant1
        createSubmission(merchant1, "Rahul Sharma", "rahul@example.com", "9876543210",
                "Sharma Textiles", "Retail", "500000", KycStatus.SUBMITTED,
                LocalDateTime.now().minusHours(30)); // Older than 24h → atRisk=true

        createSubmission(merchant1, "Rahul Sharma", "rahul@example.com", "9876543210",
                "Sharma Exports", "Export", "1000000", KycStatus.UNDER_REVIEW,
                LocalDateTime.now().minusHours(5));

        createSubmission(merchant1, "Rahul Sharma", "rahul@example.com", "9876543210",
                "Sharma Online", "E-commerce", "200000", KycStatus.DRAFT, null);

        // Sample submissions for merchant2
        createSubmission(merchant2, "Priya Mehta", "priya@example.com", "9123456780",
                "Mehta Foods", "Food & Beverage", "300000", KycStatus.APPROVED,
                LocalDateTime.now().minusDays(3));

        createSubmission(merchant2, "Priya Mehta", "priya@example.com", "9123456780",
                "Mehta Catering", "Services", "150000", KycStatus.MORE_INFO_REQUESTED,
                LocalDateTime.now().minusHours(12));

        System.out.println("✅ Sample data seeded successfully!");
        System.out.println("   Merchants: merchant1, merchant2 (password: password123)");
        System.out.println("   Reviewer:  reviewer1 (password: password123)");
        System.out.println("   H2 Console: http://localhost:8080/h2-console");
    }

    private User createUser(String username, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username).get();
        }
        User user = new User(username, passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    private void createSubmission(User merchant, String name, String email, String phone,
                                   String businessName, String businessType, String volume,
                                   KycStatus status, LocalDateTime submittedAt) {
        KycSubmission sub = new KycSubmission();
        sub.setMerchant(merchant);
        sub.setName(name);
        sub.setEmail(email);
        sub.setPhone(phone);
        sub.setBusinessName(businessName);
        sub.setBusinessType(businessType);
        sub.setExpectedMonthlyVolume(volume);
        sub.setStatus(status);
        sub.setSubmittedAt(submittedAt);
        // Placeholder file paths for demo
        sub.setPanFilePath("sample_pan.pdf");
        sub.setAadhaarFilePath("sample_aadhaar.pdf");
        sub.setBankStatementFilePath("sample_bank.pdf");
        submissionRepository.save(sub);
    }
}
