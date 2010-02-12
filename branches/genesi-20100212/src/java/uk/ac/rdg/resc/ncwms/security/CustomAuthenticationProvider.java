/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.rdg.resc.ncwms.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.ui.WebAuthenticationDetails;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationProvider.class);
    private static Map<String, String> PORTAL_USERS = new HashMap<String, String>(2) {

        {
            put("ASantokhee", "vvs");
            put("genesi", "vvs");
        }
    };

    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();

        logger.info("Authenticating user: " + username);

        Authentication ret = UserAuthenticate(authentication);

        if (ret == null) {
            return null;
        }

        // user authenticated
        logger.info("'{}' authenticated successfully. ", username);

        // create new authentication response containing the user and it's authorities
        return UserAuthorize(authentication);
    }

    /**
     * logic to authenticate a user.
     *
     * @param authentication
     * @return
     */
    private Authentication UserAuthenticate(Authentication authentication) throws AuthenticationException {
        String userId = authentication.getPrincipal().toString();
        String password = authentication.getCredentials().toString();

        WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
        if (details != null) {
            String clientIP = details.getRemoteAddress();
            String sessionId = details.getSessionId();
        }
        
        if (userId.equalsIgnoreCase("ASantokhee")) {
            GetProxyCert gp = new GetProxyCert();
            boolean resp = gp.ContactWebService(userId, "CyzpsnUZvOGreMp");

            if (resp) {
                return authentication;
            } else {
                return null;
            }
        

            /*
            //Authenticate the user
            if (PORTAL_USERS.containsKey(userId) && PORTAL_USERS.get(userId).equals(password)) {

            if (userId.equalsIgnoreCase("ASantokhee")) {
            GetProxyCert gp = new GetProxyCert();
            boolean resp = gp.ContactWebService(userId, "CyzpsnUZvOGreMp");

            if (resp) {
            return authentication;
            } else {
            return null;
            }
            } else {
            return null;
            }*/
            } else {
            return null;
            }
        }

        /**
         * This is your logic to authorize a user
         *
         * @param authentication
         * @return
         */
    private

     Authentication UserAuthorize(Authentication authentication) throws AuthenticationException {
        String userId = authentication.getPrincipal().toString();
        String password = authentication.getCredentials().toString();

        UserDetails userDetails = null;
        List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();

        // Now is your logic to authorize the user
        if ("genesi".equalsIgnoreCase(userId)) {
            roles.add(new GrantedAuthorityImpl("ROLE_USER"));
        } else if ("ASantokhee".equalsIgnoreCase(userId)) {
            roles.add(new GrantedAuthorityImpl("ROLE_USER"));
            roles.add(new GrantedAuthorityImpl("ROLE_SUPERVISOR"));
        }
        GrantedAuthority[] toReturn = new GrantedAuthorityImpl[0];
        userDetails = new User(userId, password, true, true, true, true,
                (GrantedAuthority[]) (GrantedAuthority[]) roles.toArray(toReturn));

        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                userDetails, authentication.getCredentials(), userDetails.getAuthorities());
        result.setDetails(authentication.getDetails());
        return result;
    }

    public boolean supports(Class authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
