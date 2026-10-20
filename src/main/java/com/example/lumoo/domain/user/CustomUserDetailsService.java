package com.example.lumoo.domain.user;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private com.example.lumoo.infrastructure.security.LoginAttemptService loginAttemptService;
 @Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    // Only accounts created after verification became mandatory must verify before logging in.
    boolean verificationPending = user.isEmailVerificationRequired() && !user.isEmailVerified();
    return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            !user.isSuspended(),                          // enabled
            true,                                         // accountNonExpired
            !verificationPending,                         // credentialsNonExpired → /login?unverified
            !loginAttemptService.isLocked(user.getEmail()), // accountNonLocked → /login?locked
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    );
}
}
