/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.rdg.resc.ncwms.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 *
 * @author ads
 */
public class myUserDetails implements UserDetailsService, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(myUserDetails.class);
    private VVSUser vvsUser = new VVSUser();

   private static Map<String, String> SIMPLE_USERS = new HashMap<String, String>(2) {{
        put("joe", "cocker");
        put("scott", "tiger");
    }};



    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {

        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (obj instanceof UserDetails ){
            String pwd = ((UserDetails)obj).getPassword();

        }

        if (username.equals("genesi")) {
            logger.info("User genesi has logged in");
            this.setUserPassword("vvs");
            return this.vvsUser;
        }
        throw new UsernameNotFoundException(username);
    }


        /**
     * Sets the admin password.  This is only called from
     * {@link uk.ac.rdg.resc.ncwms.config.Config#setApplicationContext}
     * and should not be called separately.
     */
    public void setUserPassword(String password) {
        this.vvsUser.password = password;
    }


    /**
     * Class to describe the vvs user: has the username "genesi"
     */
    private class VVSUser implements UserDetails, Serializable {

        private String password = null;

        @Override
        public boolean isEnabled() {

            return this.password != null;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public String getUsername() {
            return "genesi";
        }

        @Override
        public String getPassword() {
            return this.password;
        }

        /**
         * @return a single GrantedAuthority called "ROLE_USER"
         */
        @Override
        public GrantedAuthority[] getAuthorities() {

            List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();

            // Assume all users have role ROLE_USER
            roles.add(new GrantedAuthorityImpl("ROLE_USER"));

            return new GrantedAuthorityImpl[0];
        }
    }

}
