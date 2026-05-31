package com.example.lumoo.domain.user;
import com.example.lumoo.domain.user.Notification;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.NotificationRepository;
import com.example.lumoo.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Value("${app.upload.dir:/app/uploads/products}")
    private String uploadDir;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NotificationRepository notificationRepository;
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    public List<User> getAll() {
        return userRepository.findAll();
    }
    public long countAll() { return userRepository.count(); }
    public org.springframework.data.domain.Page<User> getPage(int page, int size) {
        return userRepository.findAll(
            org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("id").descending()));
    }
    public void save(User user) {
        userRepository.save(user);
    }
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
    public boolean register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) return false;
        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        userRepository.save(user);
        return true;
    }
    public void updateProfile(User user, String fullName, String phone, String address) {
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        userRepository.save(user);
    }
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_IMAGE_TYPES    = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_KYC_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".pdf");
    private static final Set<String> ALLOWED_KYC_TYPES      = Set.of("image/jpeg", "image/png", "application/pdf");
    private void validateFile(MultipartFile file, Set<String> allowedTypes, Set<String> allowedExtensions) throws IOException {
        String ct = file.getContentType();
        if (ct == null || !allowedTypes.contains(ct.toLowerCase())) {
            throw new IOException("Invalid file type.");
        }
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf(".")).toLowerCase();
            if (!allowedExtensions.contains(ext)) {
                throw new IOException("Invalid file extension.");
            }
        }
    }
    public String saveKycDoc(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_KYC_TYPES, ALLOWED_KYC_EXTENSIONS);
        String extension = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf(".")).toLowerCase();
        }
        String filename = UUID.randomUUID() + extension;
        Path dir = Paths.get(uploadDir).getParent().resolve("kyc");
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/kyc/" + filename;
    }
    public String saveAvatar(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS);
        String extension = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf(".")).toLowerCase();
        }
        String filename = UUID.randomUUID() + extension;
        Path dir = Paths.get(uploadDir).getParent().resolve("avatars");
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/avatars/" + filename;
    }
    public enum PasswordChangeResult { SUCCESS, WRONG_CURRENT, MISMATCH, TOO_SHORT }
    public PasswordChangeResult changePassword(User user, String current, String newPwd, String confirm) {
        if (!passwordEncoder.matches(current, user.getPassword())) return PasswordChangeResult.WRONG_CURRENT;
        if (!newPwd.equals(confirm)) return PasswordChangeResult.MISMATCH;
        if (newPwd.length() < 8) return PasswordChangeResult.TOO_SHORT;
        user.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(user);
        return PasswordChangeResult.SUCCESS;
    }
    public List<Notification> getAndMarkNotificationsRead(User user) {
        List<Notification> notes = notificationRepository.findByUserAndIsReadFalse(user);
        if (notes != null && !notes.isEmpty()) {
            notes.forEach(n -> n.setRead(true));
            notificationRepository.saveAll(notes);
        }
        return notes;
    }
    public List<Notification> getRecentNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .limit(10).toList();
    }
    public long countUnreadNotifications(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
    public void notifyAdmins(String message) {
        userRepository.findByRole(Role.ADMIN).forEach(admin ->
                notificationRepository.save(new Notification(message, admin)));
    }
    public boolean verifyUser(Long id) {
        return userRepository.findById(id).map(user -> {
            if (user.isVerified()) return false;
            user.setVerified(true);
            userRepository.save(user);
            notificationRepository.save(new Notification(
                "✅ Your account has been verified by LUMOO admin.", user));
            return true;
        }).orElse(false);
    }
    public boolean upgradeToVendor(Long id) {
        return userRepository.findById(id).map(user -> {
            if (user.getRole() == Role.VENDOR || user.getRole() == Role.ADMIN) return false;
            user.setRole(Role.VENDOR);
            user.setVerified(true);
            userRepository.save(user);
            notificationRepository.save(new Notification(
                "🎉 Your account has been upgraded to Vendor by LUMOO admin. Please log out and log back in to access your Vendor Hub.", user));
            return true;
        }).orElse(false);
    }
}
