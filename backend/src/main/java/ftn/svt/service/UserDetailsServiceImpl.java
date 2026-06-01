package ftn.svt.service;

import ftn.svt.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private UserService userService;

    @Autowired
    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username);
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), user.isEnabled(),
                true, true, true, getGrantedAuthorities(user)
        );
    }

   private Collection<? extends GrantedAuthority> getGrantedAuthorities(User user) {
       List<GrantedAuthority> authorities = new ArrayList<>();
       String role = "ROLE_" + user.getRole().toString();
       authorities.add(new SimpleGrantedAuthority(role));

       return authorities;
   }
}
