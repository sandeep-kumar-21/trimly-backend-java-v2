package com.trimly.api.seeder;

import com.trimly.api.model.entity.*;
import com.trimly.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Native Java Database Seeder for Trimly.
 * 
 * Automatically populates a complete, balanced test dataset covering all
 * features:
 * - Configurable user credentials via application.properties (seed.user.email &
 * seed.user.password)
 * - Multi-channel Marketing Campaigns
 * - Short Links (Custom aliases, password protection, expiration, tags, UTM
 * tracking)
 * - Designer Styled QR Codes (Classy, Extra-Rounded, Dots, Rounded, Rose,
 * Classic)
 * - Rich Analytics Clicks (142 clicks across 14 days, geolocations, devices,
 * OS, browsers, referrers)
 * 
 * Trigger manually via:
 * .\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--seed"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final UrlRepository urlRepository;
    private final QrCodeRepository qrCodeRepository;
    private final ClickRepository clickRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    @Value("${seed.user.email:demo@trimly.com}")
    private String seedEmail;

    @Value("${seed.user.password:DemoPassword@123}")
    private String seedPassword;

    @Value("${seed.user.name:Demo User}")
    private String seedName;

    @Override
    public void run(String... args) {
        boolean hasSeedArg = Arrays.asList(args).contains("--seed") || Arrays.asList(args).contains("-seed");
        boolean hasSeedProfile = Arrays.asList(environment.getActiveProfiles()).contains("seed");

        if (hasSeedArg || hasSeedProfile) {
            log.info("Starting Trimly Native Java Database Seeder...");
            runSeeder();
            log.info("Seeding completed successfully. Exiting...");
            System.exit(0);
        }
    }

    @Transactional
    public void runSeeder() {
        System.out.println("\n========================================================");
        System.out.println("       🌱 TRIMLY NATIVE JAVA DATABASE SEEDER            ");
        System.out.println("========================================================");
        System.out.println("Account Email: " + seedEmail);
        System.out.println("Password:      " + seedPassword);
        System.out.println("Full Name:     " + seedName);
        System.out.println("--------------------------------------------------------\n");

        // 1. Authenticate / Setup User
        User user = setupUser();
        Long userId = user.getId();

        // 2. Clean slate for this user
        cleanupUserData(userId);

        // 3. Seed Campaigns
        Map<String, Campaign> campaigns = seedCampaigns(userId);

        // 4. Seed Links
        Map<String, Url> urls = seedLinks(userId, campaigns);

        // 5. Seed Styled QR Codes
        seedQrCodes(userId, urls);

        // 6. Seed Analytics Telemetry Clicks
        seedAnalyticsClicks(userId, campaigns, urls);

        printSummary(userId);
    }

    private User setupUser() {
        System.out.println("🔑 Step 1: Setting up user account...");
        String normalizedEmail = seedEmail.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .name(seedName)
                        .build());

        user.setName(seedName);
        user.setPasswordHash(passwordEncoder.encode(seedPassword));
        User savedUser = userRepository.save(user);

        System.out.println("   ✓ User configured: " + savedUser.getEmail() + " (ID: " + savedUser.getId() + ")");
        return savedUser;
    }

    private void cleanupUserData(Long userId) {
        System.out.println("\n🧹 Step 2: Preparing clean slate for user...");
        jdbcTemplate.update("DELETE FROM clicks WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM qrcodes WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM urls WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM campaigns WHERE user_id = ?", userId);
        System.out.println("   ✓ Cleared old user records (clicks, qrcodes, urls, campaigns).");
    }

    private Map<String, Campaign> seedCampaigns(Long userId) {
        System.out.println("\n📢 Step 3: Seeding Marketing Campaigns...");
        Map<String, Campaign> map = new LinkedHashMap<>();

        List<Campaign> campaignList = List.of(
                Campaign.builder()
                        .userId(userId)
                        .name("Summer Launch & Growth 2026")
                        .description(
                                "Omnichannel marketing push for summer promotional offers, creator partnerships, and direct consumer engagement.")
                        .channels(new ArrayList<>(List.of("social", "email", "paid", "influencer")))
                        .build(),
                Campaign.builder()
                        .userId(userId)
                        .name("Developer Community Outreach")
                        .description("Open source advocacy, technical tutorials, and developer conference workshops.")
                        .channels(new ArrayList<>(List.of("social", "email", "qr", "community")))
                        .build(),
                Campaign.builder()
                        .userId(userId)
                        .name("Q4 Enterprise Product Launch")
                        .description(
                                "High-touch B2B enterprise outreach for sales acceleration and executive product webinars.")
                        .channels(new ArrayList<>(List.of("email", "paid", "social", "sms")))
                        .build(),
                Campaign.builder()
                        .userId(userId)
                        .name("Black Friday 2025 Retrospective")
                        .description("Archived seasonal promotional flash sale campaign.")
                        .channels(new ArrayList<>(List.of("email", "social", "sms")))
                        .build());

        for (Campaign c : campaignList) {
            Campaign saved = campaignRepository.save(c);
            map.put(saved.getName(), saved);
            System.out.println("   ✓ Created Campaign: \"" + saved.getName() + "\" (ID: " + saved.getId() + ")");
        }

        return map;
    }

    private Map<String, Url> seedLinks(Long userId, Map<String, Campaign> campaigns) {
        System.out.println("\n🔗 Step 4: Seeding Diverse Short Links...");
        Instant now = Instant.now();
        Instant thirtyDaysAhead = now.plus(Duration.ofDays(30));
        Instant fourteenDaysAgo = now.minus(Duration.ofDays(14));

        Campaign summerCamp = campaigns.get("Summer Launch & Growth 2026");
        Campaign devCamp = campaigns.get("Developer Community Outreach");
        Campaign q4Camp = campaigns.get("Q4 Enterprise Product Launch");
        Campaign bfCamp = campaigns.get("Black Friday 2025 Retrospective");

        List<Url> linkList = List.of(
                Url.builder()
                        .userId(userId)
                        .shortCode("trimly-docs")
                        .longUrl("https://docs.trimly.io/getting-started")
                        .title("Trimly Official Documentation & API Guide")
                        .tags(new ArrayList<>(List.of("docs", "api", "developer")))
                        .campaignId(devCamp != null ? devCamp.getId() : null)
                        .channel("qr")
                        .hasQR(true)
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("summer-sale")
                        .longUrl("https://store.trimly.io/summer-special-offer")
                        .title("Summer Sale - 50% Off Annual Subscriptions")
                        .tags(new ArrayList<>(List.of("marketing", "sale", "promo")))
                        .campaignId(summerCamp != null ? summerCamp.getId() : null)
                        .channel("social")
                        .utmSource("twitter")
                        .utmMedium("social")
                        .utmCampaign("summer_launch")
                        .utmContent("hero_banner")
                        .hasQR(true)
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("github-repo")
                        .longUrl("https://github.com/trimly-app/trimly-platform")
                        .title("Trimly Core Open Source Platform Repository")
                        .tags(new ArrayList<>(List.of("developer", "github", "open-source")))
                        .campaignId(devCamp != null ? devCamp.getId() : null)
                        .channel("social")
                        .hasQR(true)
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("secret-vault")
                        .longUrl("https://drive.google.com/drive/folders/trimly-secure-assets")
                        .title("Confidential Asset Vault & Brand Guidelines")
                        .tags(new ArrayList<>(List.of("security", "brand", "internal")))
                        .passwordHash(passwordEncoder.encode("Vault@2026")) // Password protected
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("live-webinar")
                        .longUrl("https://zoom.us/j/9876543210")
                        .title("Interactive Product Keynote & Live Demo")
                        .tags(new ArrayList<>(List.of("webinar", "events", "product")))
                        .campaignId(q4Camp != null ? q4Camp.getId() : null)
                        .channel("email")
                        .expiresAt(thirtyDaysAhead) // Active expiring link
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("flash-deal-2025")
                        .longUrl("https://store.trimly.io/deals/cyber-week-2025")
                        .title("Cyber Week 2025 Flash Discount (Expired)")
                        .tags(new ArrayList<>(List.of("promo", "archive", "expired")))
                        .campaignId(bfCamp != null ? bfCamp.getId() : null)
                        .channel("social")
                        .expiresAt(fourteenDaysAgo) // Expired link filter test
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("pricing-tiers")
                        .longUrl("https://trimly.io/pricing")
                        .title("Trimly Enterprise & Team Pricing Plans")
                        .tags(new ArrayList<>(List.of("pricing", "enterprise", "saas")))
                        .campaignId(q4Camp != null ? q4Camp.getId() : null)
                        .channel("paid")
                        .utmSource("google")
                        .utmMedium("cpc")
                        .utmCampaign("enterprise_launch")
                        .hasQR(true)
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("mobile-app")
                        .longUrl("https://apps.apple.com/app/trimly-smart-scanner/id123456789")
                        .title("Download Trimly iOS & Android Scanner App")
                        .tags(new ArrayList<>(List.of("mobile", "ios", "app")))
                        .hasQR(true)
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("youtube-demo")
                        .longUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                        .title("Trimly 2.0 Product Tour & Feature Walkthrough")
                        .tags(new ArrayList<>(List.of("media", "video", "youtube")))
                        .campaignId(summerCamp != null ? summerCamp.getId() : null)
                        .channel("influencer")
                        .hasQR(true)
                        .isCustomAlias(true)
                        .visibleAsLink(true)
                        .build(),
                Url.builder()
                        .userId(userId)
                        .shortCode("k8s-guide")
                        .longUrl("https://kubernetes.io/docs/concepts/overview/")
                        .title("Kubernetes Production Architecture & Setup Guide")
                        .tags(new ArrayList<>(List.of("devops", "cloud")))
                        .isCustomAlias(false)
                        .visibleAsLink(true)
                        .build());

        Map<String, Url> urlMap = new LinkedHashMap<>();
        for (Url url : linkList) {
            Url saved = urlRepository.save(url);
            urlMap.put(saved.getShortCode(), saved);
            String passInfo = saved.getPasswordHash() != null ? " [🔒 Password: Vault@2026]" : "";
            String expInfo = saved.getExpiresAt() != null
                    ? " [⏳ Expires: " + saved.getExpiresAt().toString().substring(0, 10) + "]"
                    : "";
            System.out.println("   ✓ Created Link: /" + saved.getShortCode() + " -> \"" + saved.getTitle() + "\""
                    + passInfo + expInfo);
        }

        return urlMap;
    }

    private void seedQrCodes(Long userId, Map<String, Url> urls) {
        System.out.println("\n🎨 Step 5: Seeding Designer QR Codes...");

        record QrDef(String shortCode, String dotsStyle, String cornersStyle, String cornersDotStyle, String dotsColor,
                String bgColor, String centerText, String label) {
        }

        List<QrDef> qrDefs = List.of(
                new QrDef("trimly-docs", "classy", "extra-rounded", "dot", "#1D4ED8", "#FFFFFF", "TRIMLY",
                        "Classy Concentric (Navy / Text Center)"),
                new QrDef("summer-sale", "extra-rounded", "extra-rounded", "square", "#059669", "#F0FDF4", "SALE",
                        "Extra-Rounded Modern (Emerald / Soft Mint)"),
                new QrDef("github-repo", "dots", "dot", "dot", "#7C3AED", "#FAF5FF", "GIT",
                        "Dots Geometric (Purple / Circular Corners)"),
                new QrDef("pricing-tiers", "rounded", "square", "square", "#D97706", "#FFFBEB", "PRO",
                        "Rounded Grid (Amber / Classic Corners)"),
                new QrDef("mobile-app", "classy-rounded", "extra-rounded", "dot", "#E11D48", "#FFF1F2", "APP",
                        "Classy-Rounded Elegant (Rose / Dot Corner)"),
                new QrDef("youtube-demo", "square", "square", "square", "#0F172A", "#FFFFFF", null,
                        "Monochrome Standard (Slate / Classic)"));

        for (QrDef q : qrDefs) {
            QrCode qr = QrCode.builder()
                    .userId(userId)
                    .shortCode(q.shortCode)
                    .dotsStyle(q.dotsStyle)
                    .cornersStyle(q.cornersStyle)
                    .cornersDotStyle(q.cornersDotStyle)
                    .dotsColor(q.dotsColor)
                    .backgroundColor(q.bgColor)
                    .centerText(q.centerText)
                    .build();

            QrCode savedQr = qrCodeRepository.save(qr);
            Url url = urls.get(q.shortCode);
            if (url != null) {
                url.setHasQR(true);
                url.setQrCodeId(savedQr.getId());
                urlRepository.save(url);
            }
            System.out.println("   ✓ Created QR: /" + q.shortCode + " -> " + q.label);
        }
    }

    private void seedAnalyticsClicks(Long userId, Map<String, Campaign> campaigns, Map<String, Url> urls) {
        System.out.println("\n📊 Step 6: Seeding Rich Analytics & Click Telemetry (142 Clicks)...");

        record GeoItem(String country, String city, String region, int weight) {
        }
        List<GeoItem> geoPool = List.of(
                new GeoItem("US", "San Francisco", "California", 22),
                new GeoItem("US", "New York", "New York", 18),
                new GeoItem("IN", "Bengaluru", "Karnataka", 18),
                new GeoItem("GB", "London", "Greater London", 16),
                new GeoItem("IN", "Mumbai", "Maharashtra", 12),
                new GeoItem("DE", "Berlin", "Berlin", 11),
                new GeoItem("CA", "Toronto", "Ontario", 9),
                new GeoItem("US", "Austin", "Texas", 7),
                new GeoItem("FR", "Paris", "Ile-de-France", 6),
                new GeoItem("US", "Seattle", "Washington", 5),
                new GeoItem("JP", "Tokyo", "Kanto", 4),
                new GeoItem("IN", "Delhi", "Delhi", 4),
                new GeoItem("AU", "Sydney", "New South Wales", 2));

        record DeviceItem(String deviceType, String os, String browser, int weight) {
        }
        List<DeviceItem> devicePool = List.of(
                new DeviceItem("Desktop", "macOS", "Chrome", 36),
                new DeviceItem("Desktop", "Windows", "Chrome", 24),
                new DeviceItem("Mobile", "iOS", "Safari", 30),
                new DeviceItem("Mobile", "Android", "Chrome", 14),
                new DeviceItem("Desktop", "macOS", "Safari", 12),
                new DeviceItem("Desktop", "Windows", "Edge", 10),
                new DeviceItem("Tablet", "iOS", "Safari", 8),
                new DeviceItem("Desktop", "Linux", "Firefox", 8));

        record RefItem(String referrer, String utmSource, String utmMedium, int weight) {
        }
        List<RefItem> refPool = List.of(
                new RefItem("https://www.google.com", "google", "organic", 38),
                new RefItem("https://twitter.com", "twitter", "social", 28),
                new RefItem("https://www.linkedin.com", "linkedin", "social", 24),
                new RefItem("", "direct", "none", 20),
                new RefItem("https://github.com", "github", "referral", 14),
                new RefItem("https://www.reddit.com", "reddit", "community", 10),
                new RefItem("https://www.youtube.com", "youtube", "video", 8));

        Campaign summerCamp = campaigns.get("Summer Launch & Growth 2026");
        Campaign devCamp = campaigns.get("Developer Community Outreach");
        Campaign q4Camp = campaigns.get("Q4 Enterprise Product Launch");

        record LinkWeight(String code, boolean isQr, Long campId, int weight) {
        }
        List<LinkWeight> linkPool = List.of(
                new LinkWeight("summer-sale", false, summerCamp != null ? summerCamp.getId() : null, 34),
                new LinkWeight("trimly-docs", true, devCamp != null ? devCamp.getId() : null, 28),
                new LinkWeight("pricing-tiers", false, q4Camp != null ? q4Camp.getId() : null, 24),
                new LinkWeight("mobile-app", true, null, 18),
                new LinkWeight("github-repo", false, devCamp != null ? devCamp.getId() : null, 14),
                new LinkWeight("secret-vault", false, null, 10),
                new LinkWeight("youtube-demo", true, summerCamp != null ? summerCamp.getId() : null, 8),
                new LinkWeight("live-webinar", false, q4Camp != null ? q4Camp.getId() : null, 6));

        Random random = new Random(42); // Deterministic seed
        List<Click> clickList = new ArrayList<>(142);
        Map<String, Long> linkClickCounts = new HashMap<>();
        long nowMillis = System.currentTimeMillis();

        for (int i = 0; i < 142; i++) {
            LinkWeight lw = pickWeighted(linkPool, random);
            GeoItem geo = pickWeighted(geoPool, random);
            DeviceItem dev = pickWeighted(devicePool, random);
            RefItem ref = pickWeighted(refPool, random);

            // 14-day timeline
            double skew = Math.pow(random.nextDouble(), 0.7);
            long dayOffset = (long) (skew * 14);
            long hourOffset = random.nextInt(24);
            long minOffset = random.nextInt(60);
            Instant timestamp = Instant
                    .ofEpochMilli(nowMillis - (dayOffset * 86400000L + hourOffset * 3600000L + minOffset * 60000L));

            // 68 unique visitors
            int visitorNum = (i % 68) + 1;
            String ipHash = String.format("visitor_hash_%03d", visitorNum);
            boolean isQrScan = lw.isQr || (i % 3 == 0);

            Click click = Click.builder()
                    .shortCode(lw.code)
                    .userId(userId)
                    .campaignId(lw.campId)
                    .timestamp(timestamp)
                    .ipHash(ipHash)
                    .referrer(ref.referrer)
                    .userAgent("Mozilla/5.0 (" + dev.os + ") AppleWebKit/537.36 (" + dev.browser + ")")
                    .deviceType(dev.deviceType)
                    .browser(dev.browser)
                    .os(dev.os)
                    .country(geo.country)
                    .city(geo.city)
                    .region(geo.region)
                    .isQrScan(isQrScan)
                    .utmSource(ref.utmSource)
                    .utmMedium(ref.utmMedium)
                    .utmCampaign(lw.campId != null ? "campaign_track" : "organic_track")
                    .build();

            clickList.add(click);
            linkClickCounts.put(lw.code, linkClickCounts.getOrDefault(lw.code, 0L) + 1L);
        }

        clickRepository.saveAll(clickList);

        // Update click_count on urls
        for (Map.Entry<String, Long> entry : linkClickCounts.entrySet()) {
            Url u = urls.get(entry.getKey());
            if (u != null) {
                u.setClickCount(entry.getValue());
                urlRepository.save(u);
            }
        }

        System.out.println("   ✓ Inserted 142 clicks across " + linkClickCounts.size() + " links.");
        System.out.println("   ✓ Synced 'click_count' on 'urls' table.");
    }

    private void printSummary(Long userId) {
        long totalClicks = clickRepository.countByUserId(userId);
        long uniqueVisitors = clickRepository.countDistinctVisitorsByUserId(userId);
        long qrScans = clickRepository.countByUserIdAndIsQrScanTrue(userId);
        long urlsCount = urlRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
        long qrsCount = qrCodeRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
        long campsCount = campaignRepository.findByUserIdOrderByCreatedAtDesc(userId).size();

        System.out.println("\n🔍 Step 7: Verifying Seeded Assets...");
        System.out.println("   ---------------------------------------------");
        System.out.println("   🔗 Short Links Created:  " + urlsCount);
        System.out.println("   🎨 QR Codes Created:     " + qrsCount);
        System.out.println("   📢 Campaigns Created:    " + campsCount);
        System.out.println("   📈 Total Clicks Seeded:  " + totalClicks);
        System.out.println("   👥 Unique Visitors:      " + uniqueVisitors);
        System.out.println("   📱 QR Code Scans:        " + qrScans);
        System.out.println("   ---------------------------------------------");

        System.out.println("\n========================================================");
        System.out.println("       🎉 JAVA SEEDING COMPLETED SUCCESSFULLY!          ");
        System.out.println("========================================================");
        System.out.println("Credentials configured:");
        System.out.println("Email:    " + seedEmail);
        System.out.println("Password: " + seedPassword);
        System.out.println("========================================================\n");
    }

    private <T> T pickWeighted(List<T> list, Random random) {
        int totalWeight = 0;
        for (T item : list) {
            totalWeight += getWeight(item);
        }
        int r = random.nextInt(totalWeight);
        for (T item : list) {
            int w = getWeight(item);
            if (r < w)
                return item;
            r -= w;
        }
        return list.get(0);
    }

    private int getWeight(Object item) {
        try {
            var method = item.getClass().getMethod("weight");
            return (int) method.invoke(item);
        } catch (Exception e) {
            return 1;
        }
    }
}
