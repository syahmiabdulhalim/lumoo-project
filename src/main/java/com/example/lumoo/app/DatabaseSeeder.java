package com.example.lumoo.app;

import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.blog.BlogPostRepository;
import com.example.lumoo.domain.product.ProductRepository;
import com.example.lumoo.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(2)
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private BlogPostRepository blogPostRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedVendors();
        seedProducts();
        seedBlogPosts();
    }

    // ─────────────────────────────────────────
    // VENDOR SEEDER
    // ─────────────────────────────────────────
    private void seedVendors() {
        if (userRepository.findByEmail("hassan@lumoo.my").isPresent()) return;

        User hassan = vendor("hassan@lumoo.my", "Hassan Ibrahim", "+220 771 1001", "Brikama, Gambia");
        User fatou  = vendor("fatou@lumoo.my",  "Fatou Diallo",   "+220 771 1002", "Serrekunda, Gambia");
        User omar   = vendor("omar@lumoo.my",   "Omar Bah",       "+220 771 1003", "Banjul, Gambia");

        userRepository.saveAll(List.of(hassan, fatou, omar));
    }

    private User vendor(String email, String fullName, String phone, String address) {
        User u = new User();
        u.setEmail(email);
        u.setUsername(email);
        u.setPassword(passwordEncoder.encode("vendor123"));
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setAddress(address);
        u.setRole(Role.VENDOR);
        u.setVerified(true);
        return u;
    }

    // ─────────────────────────────────────────
    // PRODUCT SEEDER  (18 products, all unique images)
    // ─────────────────────────────────────────
    private void seedProducts() {
        if (productRepository.count() > 0) return;

        User hassan = userRepository.findByEmail("hassan@lumoo.my").orElse(null);
        User fatou  = userRepository.findByEmail("fatou@lumoo.my").orElse(null);
        User omar   = userRepository.findByEmail("omar@lumoo.my").orElse(null);
        if (hassan == null || fatou == null || omar == null) return;

        productRepository.saveAll(List.of(

            // ── Hassan Ibrahim — Roofing, Cement, Building ──────────────
            product("Decra Stone Coated Roofing Sheet", "Roofing", 450.00,
                "Premium stone-coated steel roofing sheet. Durable, weather-resistant, 30-year warranty. Ideal for residential and commercial builds.",
                "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80", hassan),

            product("Corrugated Iron Sheet 26 Gauge (6ft)", "Roofing", 85.00,
                "Standard corrugated iron sheet, 26 gauge, 6ft length. Suitable for residential roofing and wall cladding.",
                "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&q=80", hassan),

            product("Portland Cement 50kg Bag", "Cement", 28.00,
                "OPC Portland Cement 50kg bag. High-strength, fast-setting. Suitable for foundations, plastering and all general construction work.",
                "https://images.unsplash.com/photo-1590012314607-cda9d9b699ae?w=800&q=80", hassan),

            product("Ready-Mix Sand & Cement Mortar 25kg", "Cement", 12.00,
                "Pre-blended dry mortar mix, 25kg bag. Just add water. For bricklaying, blockwork and general repairs. BS EN 998-2 compliant.",
                "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&q=80", hassan),

            product("Hollow Concrete Block 6 inch", "Building", 4.50,
                "Standard 6-inch hollow concrete block. Lightweight and strong. Suitable for load-bearing and non-load-bearing walls.",
                "https://images.unsplash.com/photo-1565008447742-97f6f38c985c?w=800&q=80", hassan),

            product("Steel Rebar 12mm x 6m (Grade 60)", "Building", 65.00,
                "High-yield deformed steel reinforcement bar. 12mm diameter, 6m length, Grade 60. For reinforced concrete columns, beams and slabs.",
                "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=800&q=80", hassan),

            // ── Fatou Diallo — Electrical, Plumbing ─────────────────────
            product("2.5mm Twin & Earth Cable (50m Roll)", "Electrical", 95.00,
                "2.5mm twin and earth PVC insulated cable, 50m roll. For domestic ring main and radial circuits up to 20A.",
                "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&q=80", fatou),

            product("MCB Circuit Breaker 32A Single Pole", "Electrical", 22.00,
                "Type B single-pole MCB, 32A, DIN rail mounted. Overload and short-circuit protection for domestic circuits.",
                "https://images.unsplash.com/photo-1509391366636-9f525df0c76e?w=800&q=80", fatou),

            product("LED Surface Panel Light 40W (600x600)", "Electrical", 55.00,
                "40W LED recessed panel light, 600x600mm, cool white 6000K, 3600 lm. Suitable for offices, shops and commercial ceilings.",
                "https://images.unsplash.com/photo-1558618047-f9d192318ef1?w=800&q=80", fatou),

            product("uPVC Pressure Pipe 4 inch x 6m (Class E)", "Plumbing", 38.00,
                "4-inch diameter uPVC pressure pipe, 6m length, Class E rated. Suitable for water supply mains and drainage systems.",
                "https://images.unsplash.com/photo-1585771724684-38269d6639fd?w=800&q=80", fatou),

            product("Brass Full-Bore Ball Valve 1 inch BSP", "Plumbing", 14.00,
                "Full-bore brass ball valve, DN25 (1-inch BSP), lead-free. Rated for hot and cold water up to 25 bar. Quarter-turn operation.",
                "https://images.unsplash.com/photo-1574169208507-84376144848b?w=800&q=80", fatou),

            product("Polyethylene Water Storage Tank 1000L", "Plumbing", 320.00,
                "1000-litre rotomoulded polyethylene tank. UV-stabilised, food-grade, supplied with lid, inlet and outlet fittings.",
                "https://images.unsplash.com/photo-1503736334956-4c8f8e92946d?w=800&q=80", fatou),

            // ── Omar Bah — Timber, Paint, Hardware ──────────────────────
            product("Hardwood Timber 2x4 inch x 12ft", "Timber", 32.00,
                "Kiln-dried structural hardwood, 2x4 inch, 12ft length. Suitable for roof trusses, floor joists and general framing.",
                "https://images.unsplash.com/photo-1541123437800-1bb1317badc2?w=800&q=80", omar),

            product("Marine Plywood Sheet 18mm (8x4ft)", "Timber", 55.00,
                "18mm WBP marine-grade plywood, 8x4ft sheet, BB/BB grade faces. Fully waterproof bond. For formwork, shuttering and furniture.",
                "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80", omar),

            product("Dulux Weathershield Exterior Paint 20L", "Paint", 110.00,
                "Premium exterior masonry paint, smooth finish, 20L. 15-year weather resistance guarantee. Available in 50+ colours.",
                "https://images.unsplash.com/photo-1562259929-b4e1fd3aef09?w=800&q=80", omar),

            product("Interior Matt Emulsion Paint 5L (Brilliant White)", "Paint", 28.00,
                "High-coverage interior matt emulsion, 5L. One-coat coverage up to 65m². Wipeable, low-odour, quick-dry formula.",
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&q=80", omar),

            product("Stainless Steel Anchor Bolt M10 x 100mm (Box 50)", "Hardware", 18.00,
                "M10 stainless steel sleeve anchor bolts, 100mm length, box of 50. For fixing into concrete, brick and masonry up to 25kN shear.",
                "https://images.unsplash.com/photo-1572981779307-38b8cabb2407?w=800&q=80", omar),

            product("Angle Grinder 115mm 900W with Guard & Disc", "Hardware", 145.00,
                "115mm angle grinder, 900W motor, 11,000 RPM no-load speed. Includes 115mm cutting disc, safety guard and side handle.",
                "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?w=800&q=80", omar)
        ));
    }

    private Product product(String name, String category, double price,
                            String description, String imageUrl, User vendor) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(price);
        p.setDescription(description);
        p.setImageUrl(imageUrl);
        p.setVendor(vendor);
        p.setApproved(true);
        p.setImageApproved(true);
        return p;
    }

    // ─────────────────────────────────────────
    // BLOG SEEDER  (8 posts, all unique images)
    // ─────────────────────────────────────────
    private void seedBlogPosts() {
        if (blogPostRepository.existsBySlug("how-to-choose-roofing-material-gambia")) return;

        blogPostRepository.saveAll(List.of(

            post("How to Choose the Right Roofing Material for Gambia's Climate",
                "how-to-choose-roofing-material-gambia",
                "Gambia's tropical climate demands roofing materials that handle heavy rains, intense heat and high humidity. Here's what to consider before you buy.",
                "<p>Choosing the right roofing material is one of the most important decisions in any construction project.</p>" +
                "<h2>Key Factors</h2>" +
                "<p><strong>Heat Resistance:</strong> Stone-coated steel and clay tiles reflect heat better than plain iron sheets.</p>" +
                "<p><strong>Rain Performance:</strong> The wet season brings heavy downpours. Your roof needs proper pitch and sealed overlaps to prevent leaks.</p>" +
                "<p><strong>Durability:</strong> Stone-coated steel lasts 30–50 years vs 10–15 years for corrugated iron.</p>" +
                "<h2>Our Recommendation</h2>" +
                "<p>For long-term value, invest in stone-coated steel or fibre cement tiles. Browse our Roofing category for options from verified vendors across Gambia.</p>",
                "Roofing", "roofing,climate,materials,gambia",
                "https://images.unsplash.com/photo-1524634126442-357e0eac3c14?w=1200&q=80",
                "Hassan Ibrahim", 4, 14),

            post("A Complete Guide to Mixing Concrete on Site",
                "complete-guide-mixing-concrete-site",
                "Get the mix right and your structure stands for decades. Get it wrong and you risk costly failures. Ratios, water content and common mistakes explained.",
                "<p>Concrete is the backbone of most construction projects. Yet improper mixing remains one of the most common causes of structural failure.</p>" +
                "<h2>The Golden Ratio</h2>" +
                "<p>For general work: 1:2:4 (cement:sand:aggregate). For foundations and columns: 1:1.5:3.</p>" +
                "<h2>Water is Critical</h2>" +
                "<p>Keep the water-cement ratio between 0.45 and 0.60. If it flows off your shovel easily, there's too much water.</p>" +
                "<h2>Mixing Tips</h2>" +
                "<ul><li>Mix dry ingredients first, then add water gradually</li>" +
                "<li>Use clean, uncontaminated water only</li>" +
                "<li>Mix for at least 2 minutes for uniformity</li>" +
                "<li>Pour and compact within 30 minutes of mixing</li></ul>" +
                "<p>Never add water to concrete that has started to set. Keep concrete moist for 7 days after pouring to cure properly.</p>",
                "Cement", "cement,concrete,mixing,construction",
                "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=1200&q=80",
                "LUMOO Editorial", 5, 10),

            post("Electrical Safety in Construction: What Every Builder Must Know",
                "electrical-safety-construction-builders",
                "Electrical accidents on construction sites are preventable. These are the non-negotiables every contractor and site worker must follow.",
                "<p>Electrical hazards are among the leading causes of injury on construction sites. Most incidents are entirely preventable.</p>" +
                "<h2>Cable Selection</h2>" +
                "<p>Use the correct gauge for the load: 1.5mm for lighting, 2.5mm for sockets, 4mm for heavy appliances.</p>" +
                "<h2>Circuit Protection</h2>" +
                "<p>Every circuit must be protected by a correctly rated MCB. Never substitute fuses — they don't respond fast enough.</p>" +
                "<h2>Earthing and Bonding</h2>" +
                "<p>All metalwork must be bonded to earth to provide a safe fault-current path and trigger the MCB.</p>" +
                "<h2>Safe Working Practices</h2>" +
                "<ul><li>Always isolate supply before working on any circuit</li>" +
                "<li>Test with a voltage tester — never assume a circuit is dead</li>" +
                "<li>Use double-insulated tools on site</li>" +
                "<li>Keep cables off the floor and away from water</li></ul>",
                "Electrical", "electrical,safety,construction,wiring",
                "https://images.unsplash.com/photo-1603796846097-bee99e4a601f?w=1200&q=80",
                "Fatou Diallo", 5, 7),

            post("Top 5 Plumbing Mistakes Builders Make (And How to Fix Them)",
                "top-5-plumbing-mistakes-builders",
                "Plumbing errors are expensive to fix after walls are plastered. Avoid these five mistakes before they become major problems.",
                "<p>Plumbing installed correctly lasts decades. Installed incorrectly, it causes water damage, mould and costly repairs.</p>" +
                "<h2>1. Wrong Pipe Grade</h2>" +
                "<p>Using drainage-grade PVC for pressure supply is a common error. Use Class E or Class C pipes for mains pressure.</p>" +
                "<h2>2. Inadequate Fall on Drainage</h2>" +
                "<p>Drainage pipes need a minimum 1:40 gradient. Too flat and waste won't clear; too steep and solids block.</p>" +
                "<h2>3. Not Pressure Testing Before Plastering</h2>" +
                "<p>Test at 1.5× working pressure before closing walls. A leak found after plastering costs ten times more to fix.</p>" +
                "<h2>4. Skipping Isolation Valves</h2>" +
                "<p>Every appliance needs an isolation valve so you can service it without shutting off the whole building.</p>" +
                "<h2>5. No Access Panels</h2>" +
                "<p>Concealed pipes must have access panels at every joint — a maintenance and regulatory requirement.</p>",
                "Plumbing", "plumbing,mistakes,construction,pipes",
                "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=1200&q=80",
                "Fatou Diallo", 5, 5),

            post("Why Quality Timber Makes or Breaks Your Build",
                "why-quality-timber-matters-construction",
                "Not all timber is equal. Moisture content, grade and species all affect how your structure performs. Here's what to look for when buying.",
                "<p>Timber is used in roof trusses, floor joists, formwork, doors and windows. Quality has a direct impact on longevity.</p>" +
                "<h2>Moisture Content</h2>" +
                "<p>Use kiln-dried timber below 19% moisture for structural applications. Wet timber shrinks, warps and cracks.</p>" +
                "<h2>Timber Grade</h2>" +
                "<p>Use C24 grade for roof trusses and floor joists. C16 is acceptable for non-structural partitions and formwork.</p>" +
                "<h2>Species Selection</h2>" +
                "<p>Hardwoods like iroko are naturally durable — ideal for external joinery. Softwoods work well for internal framing when protected from moisture.</p>" +
                "<h2>What to Inspect on Delivery</h2>" +
                "<ul><li>Large knots in structural positions significantly weaken timber</li>" +
                "<li>Reject bowed timber — it's hard to work with and weakens the frame</li>" +
                "<li>Check for small holes or sawdust indicating insect infestation</li>" +
                "<li>Reject timber with blue staining — fungal growth from high moisture</li></ul>",
                "Timber", "timber,wood,construction,materials",
                "https://images.unsplash.com/photo-1508450859948-4e04fabaa4ea?w=1200&q=80",
                "Omar Bah", 6, 3),

            post("Choosing the Right Paint for Gambia's Humid Climate",
                "choosing-paint-gambia-humid-climate",
                "Interior paint that works in a temperate climate can blister and peel in Gambia's heat and humidity. Here's how to choose correctly.",
                "<p>Paint selection in a tropical climate is different from a temperate one. Humidity, UV exposure and temperature swings all affect performance.</p>" +
                "<h2>Exterior Surfaces</h2>" +
                "<p>Use a masonry paint with built-in fungicide and weather resistance — at minimum a 10-year product. Thin first coats and build up layers for best adhesion.</p>" +
                "<h2>Interior Surfaces</h2>" +
                "<p>Use a breathable, washable emulsion. Avoid sealed vinyl paints in humid rooms — they trap moisture behind the film, causing bubbling.</p>" +
                "<h2>Surface Preparation is Everything</h2>" +
                "<p>Painting over damp or dusty surfaces guarantees early failure. Always seal new plaster with a diluted mist coat and allow to fully dry before applying finish coats.</p>" +
                "<h2>Recommended Products</h2>" +
                "<p>Dulux Weathershield for exterior work. For interiors, a quality matt emulsion gives the best coverage and allows walls to breathe in humid conditions.</p>",
                "Paint", "paint,exterior,interior,humid,climate",
                "https://images.unsplash.com/photo-1553361371-9b4b9f85b9cf?w=1200&q=80",
                "Omar Bah", 4, 2),

            post("Steel vs Timber Roof Trusses: Which is Right for Your Project?",
                "steel-vs-timber-roof-trusses-comparison",
                "Both materials can form an excellent roof structure, but the right choice depends on span, budget, climate exposure and build speed. We compare both.",
                "<p>Roof trusses are the skeleton of your roof. Choosing the wrong material can lead to costly problems down the line.</p>" +
                "<h2>Timber Trusses</h2>" +
                "<p><strong>Pros:</strong> Lower cost, easier to cut and modify on site, good thermal performance. <strong>Cons:</strong> Susceptible to termites and moisture if not properly treated. Requires regular inspection.</p>" +
                "<h2>Steel Trusses</h2>" +
                "<p><strong>Pros:</strong> Termite-proof, longer spans without intermediate supports, dimensionally stable. <strong>Cons:</strong> Higher material cost, requires welding equipment and skilled labour, conducts heat.</p>" +
                "<h2>Our Verdict</h2>" +
                "<p>For spans under 8m with a good roof covering — timber treated with preservative is excellent value. For wider spans, commercial buildings or coastal locations where termite risk is high — steel wins.</p>",
                "Building", "steel,timber,roof,trusses,structure",
                "https://images.unsplash.com/photo-1621905252472-943afaa20e20?w=1200&q=80",
                "LUMOO Editorial", 5, 1),

            post("How to Read a Construction Materials Price List",
                "how-to-read-construction-materials-price-list",
                "Vendor quotes can be confusing. Here's how to decode unit prices, understand what's included and compare quotes accurately.",
                "<p>Getting three quotes is standard advice, but comparing them accurately requires understanding what each line item means.</p>" +
                "<h2>Unit of Measure Matters</h2>" +
                "<p>Cement is priced per bag (50kg). Rebar per tonne or per length. Timber per linear metre or per piece. Always confirm the unit before comparing prices.</p>" +
                "<h2>What's Included in the Price?</h2>" +
                "<p>Some vendors quote ex-depot (you collect). Others include delivery. A quote that looks RM50 cheaper may cost more once you add transport.</p>" +
                "<h2>Ask About Minimum Orders</h2>" +
                "<p>Many materials have minimum order quantities. Buying exactly what you need for a small job may attract a premium. Plan ahead and batch your orders.</p>" +
                "<h2>Price Validity</h2>" +
                "<p>Material prices — especially steel and cement — fluctuate. Always confirm the price is valid on the day of delivery, not just the day of quoting.</p>" +
                "<p>On LUMOO, all product prices are updated by vendors in real time. Compare across stores before placing your order.</p>",
                "Guides", "prices,buying,materials,tips,guide",
                "https://images.unsplash.com/photo-1534611592264-d2e5e1afe14e?w=1200&q=80",
                "LUMOO Editorial", 4, 1)
        ));
    }

    private BlogPost post(String title, String slug, String excerpt, String content,
                          String category, String tags, String imageUrl,
                          String author, int readingTime, int daysAgo) {
        BlogPost p = new BlogPost();
        p.setTitle(title);
        p.setSlug(slug);
        p.setExcerpt(excerpt);
        p.setContent(content);
        p.setCategory(category);
        p.setTags(tags);
        p.setFeaturedImageUrl(imageUrl);
        p.setAuthor(author);
        p.setPublished(true);
        p.setPublishedAt(LocalDateTime.now().minusDays(daysAgo));
        p.setReadingTimeMinutes(readingTime);
        return p;
    }
}
