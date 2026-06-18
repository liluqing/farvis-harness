package com.example.security;

import com.example.entity.User;
import com.example.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter 模板。
 *
 * 约定：
 *  1. 继承 OncePerRequestFilter，确保每个请求只执行一次
 *  2. 从 Authorization header 提取 Bearer token
 *  3. 验证 token 后加载用户（含角色），设置 SecurityContext
 *  4. Filter 在事务外执行，访问 LAZY 关联必须用 fetch join
 *
 * IMPORTANT: Security Filter 在事务外执行，访问 LAZY 关联必须用 fetch join。
 * 如果使用 findByUsername() 而非 findByUsernameWithRoles()，
 * 后续访问 user.getRoles() 将抛出 LazyInitializationException。
 * Repository 中必须定义：
 *   @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
 *   Optional<User> findByUsernameWithRoles(@Param("username") String username);
 *
 * 使用方式：Phase 3 切片的 Security 层按此模板生成。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // IMPORTANT: Security Filter 在事务外执行，访问 LAZY 关联必须用 fetch join
            // 这里使用 findByUsernameWithRoles 而非 findByUsername，避免 LazyInitializationException
            userRepository.findByUsernameWithRoles(username).ifPresent(user -> {
                if (user.isEnabled()) {
                    List<String> roles = jwtTokenProvider.getRolesFromToken(token);
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
