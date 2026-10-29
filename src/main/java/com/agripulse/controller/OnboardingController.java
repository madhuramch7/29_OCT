package com.agripulse.controller;

import com.agripulse.entity.FarmerProfile;
import com.agripulse.entity.UserAccount;
import com.agripulse.repository.FarmerProfileRepository;
import com.agripulse.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/onboarding", "/api/v1/onboarding"})
public class OnboardingController {

    private final UserAccountRepository userRepo;
    private final FarmerProfileRepository profileRepo;

    public OnboardingController(UserAccountRepository userRepo, FarmerProfileRepository profileRepo) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
    }

    @PostMapping({"/submit", "/register"})
    public ResponseEntity<?> submit(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        Optional<UserAccount> userOpt = userRepo.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }
        UserAccount user = userOpt.get();

        FarmerProfile profile = profileRepo.findByUserId(user.getId()).orElseGet(FarmerProfile::new);
        profile.setUserId(user.getId());
        if (profile.getFullName() == null) profile.setFullName(user.getEmail());
        profile.setState(str(body.get("state")));
        profile.setDistrict(str(body.get("district")));
        profile.setSoilType(str(body.get("soilType")));
        profile.setFarmSize(num(body.get("farmSize")));
        profileRepo.save(profile);

        user.setOnboardingComplete(true);
        userRepo.save(user);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Onboarding complete");
        res.put("profileId", profile.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        return userRepo.findByEmail(principal.getName())
                .flatMap(u -> profileRepo.findByUserId(u.getId()))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Profile not found")));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static BigDecimal num(Object o) {
        try {
            return o == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(o));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}