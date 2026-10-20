package com.example.lumoo.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Sends the user to a login message that matches why the login was refused. */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String target = "/login?error";
        if (exception instanceof LockedException) target = "/login?locked";
        else if (exception instanceof CredentialsExpiredException) target = "/login?unverified";
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
