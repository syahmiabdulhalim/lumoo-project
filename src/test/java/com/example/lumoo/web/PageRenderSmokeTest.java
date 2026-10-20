package com.example.lumoo.web;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
/**
 * Renders every main page through the real app (H2 + security) and fails on any template error.
 * With -Dsnapshot.dir=... it also writes each page's HTML there, to diff before/after template refactors.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PageRenderSmokeTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @BeforeEach
    void accounts() {
        account("buyer@test.com", Role.USER);
        account("vendor@test.com", Role.VENDOR);
        account("admin@test.com", Role.ADMIN);
    }
    private void account(String email, Role role) {
        if (users.findByEmail(email).isPresent()) return;
        User u = new User();
        u.setEmail(email);
        u.setUsername(email);
        u.setPassword("x");
        u.setRole(role);
        u.setEmailVerified(true);
        users.save(u);
    }
    private void render(String url, String email, String role) throws Exception {
        var req = get(url);
        if (email != null) req = req.with(user(email).roles(role));
        MvcResult r = mvc.perform(req).andReturn();
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(200, r.getResponse().getStatus(), url + " → " + r.getResponse().getStatus() + " " + r.getResponse().getRedirectedUrl());
        assertTrue(body.contains("</html>") || body.contains("</div>"), url + " did not render completely");
        assertFalse(body.contains("cdn.tailwindcss.com"), url + " still loads the Tailwind CDN");
        String dir = System.getProperty("snapshot.dir");
        if (dir != null) {
            Path p = Path.of(dir, (role == null ? "public" : role) + url.replace('/', '_') + ".html");
            Files.createDirectories(p.getParent());
            Files.writeString(p, body);
        }
    }
    @ParameterizedTest
    @ValueSource(strings = {"/", "/login", "/register", "/forgot-password", "/stores", "/privacy-policy", "/terms",
            "/cookie-policy", "/returns", "/shipping", "/careers", "/blog", "/partnerships", "/about", "/track"})
    void publicPages(String url) throws Exception { render(url, null, null); }
    @ParameterizedTest
    @ValueSource(strings = {"/cart", "/buyer/dashboard", "/profile", "/settings", "/buyer/vendorapply"})
    void buyerPages(String url) throws Exception { render(url, "buyer@test.com", "USER"); }
    @ParameterizedTest
    @ValueSource(strings = {"/vendor/dashboard", "/vendor/add-product"})
    void vendorPages(String url) throws Exception { render(url, "vendor@test.com", "VENDOR"); }
    @ParameterizedTest
    @ValueSource(strings = {"/admin/dashboard", "/admin/sections/inventory", "/admin/sections/moderation",
            "/admin/sections/orders", "/admin/sections/users", "/admin/audit-logs", "/admin/settings", "/admin/promos",
            "/admin/breaches", "/admin/blog", "/admin/blog/new", "/admin/careers", "/admin/careers/new",
            "/admin/partnerships", "/admin/riders", "/admin/shipping", "/admin/dispatch", "/admin/about"})
    void adminPages(String url) throws Exception { render(url, "admin@test.com", "ADMIN"); }
    @org.junit.jupiter.api.Test
    void builtTailwindCss_isServed_andCoversArbitraryValues() throws Exception {
        MvcResult r = mvc.perform(get("/css/tailwind.css")).andReturn();
        assertEquals(200, r.getResponse().getStatus(), "run scripts/build-css.sh (the Maven build does this)");
        String css = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(css.contains(".text-\\[9px\\]"), "arbitrary values must be compiled");
        assertTrue(css.contains(".bg-\\[\\#1a2336\\]"));
    }
}
