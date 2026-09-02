package com.taxedge.modules.user.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.user.dto.UpdateUserRequest;
import com.taxedge.modules.user.dto.UserDTO;
import com.taxedge.modules.user.entity.User;
import com.taxedge.modules.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security authenticates by mobile number (used as the "username").
 * Password is optional in OTP-only accounts; a placeholder empty string is fine
 * because JWT bypasses password verification once issued.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String mobile) throws UsernameNotFoundException {
        User u = repo.findByMobile(mobile)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + mobile));
        return new org.springframework.security.core.userdetails.User(
                u.getMobile(),
                u.getPassword() == null ? "" : u.getPassword(),
                u.isEnabled(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
    }

    public User getByMobile(String mobile) {
        return repo.findByMobile(mobile)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    public List<UserDTO> listAll() {
        return repo.findAll().stream().map(UserDTO::from).toList();
    }

    public UserDTO get(Long id) {
        return UserDTO.from(repo.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND)));
    }

    public UserDTO update(String mobile, UpdateUserRequest req) {
        User u = getByMobile(mobile);
        if (req.getFullName() != null) u.setFullName(req.getFullName());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        if (req.getAddress() != null) u.setAddress(req.getAddress());
        if (req.getAvatarUrl() != null) u.setAvatarUrl(req.getAvatarUrl());
        return UserDTO.from(repo.save(u));
    }

    public void disable(Long id) {
        User u = repo.findById(id).orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        u.setEnabled(false);
        repo.save(u);
    }
}
