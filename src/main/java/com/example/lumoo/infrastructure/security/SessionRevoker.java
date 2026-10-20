package com.example.lumoo.infrastructure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** Ends every login session of a user, e.g. after a password reset or suspension. */
@Component
public class SessionRevoker {

    @Autowired private SessionRegistry sessionRegistry;

    public void expireAllSessions(String email) {
        if (email == null) return;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails ud && ud.getUsername().equalsIgnoreCase(email)) {
                sessionRegistry.getAllSessions(principal, false).forEach(s -> s.expireNow());
            }
        }
    }
}
