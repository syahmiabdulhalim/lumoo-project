package com.example.lumoo.service;

import com.example.lumoo.model.Notification;
import com.example.lumoo.model.Role;
import com.example.lumoo.model.User;
import com.example.lumoo.repository.NotificationRepository;
import com.example.lumoo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Value("${app.upload.dir:/app/uploads/products}")
    private String uploadDir;

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NotificationRepository notificationRepository;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAll() {
        return userRepository.findAll();
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

    public String saveAvatar(MultipartFile file) throws IOException {
        String extension = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf("."));
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
        if (newPwd.length() < 6) return PasswordChangeResult.TOO_SHORT;
        user.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(user);
        return PasswordChangeResult.SUCCESS;
    }

    public List<Notification> getAndMarkNotificationsRead(User user) {
        List<Notification> notes = notificationRepository.findByUser(user);
        if (notes != null && !notes.isEmpty()) {
            notes.forEach(n -> n.setRead(true));
            notificationRepository.saveAll(notes);
        }
        return notes;
    }
}
