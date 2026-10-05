package principio.kys.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import principio.kys.model.Usuario;
import principio.kys.repository.UsuarioRepository;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        if (usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            throw new UsernameNotFoundException("Usuario sin rol asignado: " + username);
        }
        boolean locked = Boolean.FALSE.equals(usuario.getEstado());

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(Collections.singletonList(() -> "ROLE_" + usuario.getRol().getNombre().toUpperCase()))
                .accountLocked(locked)
                .build();
    }
}
