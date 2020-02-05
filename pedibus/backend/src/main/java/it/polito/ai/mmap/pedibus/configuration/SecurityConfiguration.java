package it.polito.ai.mmap.pedibus.configuration;

import it.polito.ai.mmap.pedibus.services.JwtTokenService;
import it.polito.ai.mmap.pedibus.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Autowired
    UserService userService;

    @Autowired
    JwtTokenService jwtTokenService;

    @Autowired
    PasswordEncoder passwordEncoder;


    /**
     * Si specifica come si accede alle risorse
     *
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "**").permitAll()//allow CORS option calls
                .and()
                .authorizeRequests()
                .antMatchers("/messages/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/images/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/css/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/js/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/register/**").denyAll() // Nessuno può più registrarsi da solo
                .and()
                .authorizeRequests()
                .antMatchers("/login").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/confirm/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/recover/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/new-user/**").permitAll()
                .and()//TODO remove
                .authorizeRequests()
                .antMatchers("/debug/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/admin/users/**").access("hasAnyRole('ADMIN','SYSTEM-ADMIN')")
                .and()
                .authorizeRequests()
                .antMatchers("/admin/lines/**").access("hasAnyRole('ADMIN','SYSTEM-ADMIN')")
                .and()
                .authorizeRequests()
                .antMatchers("/sysadmin/**").access("hasAnyRole('SYSTEM-ADMIN')")
                .and()
                .authorizeRequests()
                .antMatchers("/reservations/verso/**").access("hasAnyRole('ADMIN','SYSTEM-ADMIN','GUIDE')")
                .and()
                .authorizeRequests()
                .antMatchers("/reservations/handled/**").access("hasAnyRole('ADMIN','SYSTEM-ADMIN','GUIDE')")
                .and()
                .authorizeRequests()
                .antMatchers("/reservations/arrived/**").access("hasAnyRole('ADMIN','SYSTEM-ADMIN','GUIDE')")
                .and()
                .authorizeRequests()
                .antMatchers("/reservations/assente/**").access("hasAnyRole('ADMIN','SYSTEM-ADMIN','GUIDE')")
                .and()
                .authorizeRequests()
                .antMatchers("/reservations/restore/**").access("hasAnyRole('ADMIN','SYSTEM-ADMIN','GUIDE')")
                .and()
                .authorizeRequests()
                .antMatchers("/notreservations/**").access("hasAnyRole('GUIDE','ADMIN','SYSTEM-ADMIN')")
                .and()
                .authorizeRequests()
                .antMatchers("/reservations/dump/**").access("hasAnyRole('ADMIN','SYSTEM-ADMIN')") //todo permettere anche alle guide?
                .and()
                .authorizeRequests()
                .antMatchers("/children/stops/**").access("hasAnyRole('USER')")
                .and()
                .authorizeRequests()
                .antMatchers("/disp/**").access("hasAnyRole('GUIDE','ADMIN','SYSTEM-ADMIN')")
                .and()
                .authorizeRequests()
                .antMatchers("/turno/**").access("hasAnyRole('GUIDE','ADMIN','SYSTEM-ADMIN')")
                .and()
                .csrf().disable()
                .authorizeRequests().anyRequest().authenticated()
                .and().httpBasic()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().apply(new JwtConfigurer(jwtTokenService));
    }

    /**
     * Invece di usare il service standard usiamo un UserDetailsService da noi definito
     *
     * @param builder
     * @throws Exception
     */
    @Override
    public void configure(AuthenticationManagerBuilder builder) throws Exception {
        builder.userDetailsService(userService).passwordEncoder(passwordEncoder);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }



}
