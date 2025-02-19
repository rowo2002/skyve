package org.skyve.impl.web.spring;

import org.skyve.impl.util.UtilImpl;
import org.skyve.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
//import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
//import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
//import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

/**
 * This class supplies named spring beans to the OOTB security.xml
 */
@Configuration
@Import(SkyveSpringSecurityConfig.class)
@EnableWebSecurity
public class SpringSecurityConfig {
	@Autowired
	private SkyveSpringSecurity skyve;
	
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests()
				// Permit H2 servlet if enabled
				.requestMatchers("/h2/**").permitAll()
				// Enable access to all rest endpoints as these will have Servlet Filters to secure.
				.requestMatchers("/rest/**").permitAll()
				// Permit the login servlet resource
				.requestMatchers(HttpMethod.GET, "/login", "/loggedOut").permitAll()
				// Permit the spring login mechanism and the SC JS login mechanism
				.requestMatchers(HttpMethod.POST, "/loginAttempt", "/smartClientJavascriptLogin").permitAll()
				// Permit the reset resources
				.requestMatchers("/pages/requestPasswordReset.jsp", "/pages/resetPassword.jsp").permitAll()
				// Permit home.jsp as it controls access to public and private pages itself
				.requestMatchers("/home.jsp").permitAll()
				// Permit device.jsp as it forwards to home.jsp
				.requestMatchers("/device.jsp").permitAll()
				// Permit the health servlet resource
				.requestMatchers("/health").permitAll()
				// Secure the loggedIn.jsp so that redirect occurs after login
				.requestMatchers("/loggedIn.jsp").authenticated()
				// Secure the system JSPs and HTMLs
				.requestMatchers("/pages/changePassword.jsp", "/pages/htmlEdit/browseImages.jsp", "/pages/map/geolocate.jsp").authenticated()
				// Do not secure the home servlet as this ensures the right login page is displayed
				.requestMatchers("/home").permitAll()
				// Do not secure faces pages as they are secured by a FacesSecurityFilter
				.requestMatchers("/**/*.xhtml").permitAll()
				// Secure dynamic image URLs
				.requestMatchers("/images/dynamic.*").authenticated()
				// Secure all report URLs
				.requestMatchers("/report", "/export").authenticated()
				// Secure chart servlet
				.requestMatchers("/chart").authenticated()
				// Secure Image Servlet for HTML reporting through Jasper
				.requestMatchers("/image").authenticated()
				// Secure customer resource servlet
				.requestMatchers("/resource", "/content").authenticated()
				// Secure meta data servlet
				.requestMatchers("/meta").authenticated()
				// Secure SC edit view servlet
				.requestMatchers("/smartedit").authenticated()
				// Secure SC list view servlet
				.requestMatchers("/smartlist").authenticated()
				// Secure SC view generation servlet
				.requestMatchers("/smartgen").authenticated()
				// Secure SC snap servlet
				.requestMatchers("/smartsnap").authenticated()
				// Secure SC tag servlet
				.requestMatchers("/smarttag").authenticated()
				// Secure SC complete servlet
				.requestMatchers("/smartcomplete").authenticated()
				// Secure SC text search servlet
				.requestMatchers("/smartsearch").authenticated()
				// Secure Prime initialisation servlet
				.requestMatchers("/primeinit").authenticated()
				// Secure map servlet
				.requestMatchers("/map").authenticated()
				// Secure the Bizport Export Servlet
				.requestMatchers("/bizexport.*").authenticated()
				// Secure the Download Servlet
				.requestMatchers("/download").authenticated()
				// Secure the Push endpoint
				.requestMatchers("/omnifaces.push/**").authenticated()
				// Do not Secure trackmate servlet - it handles authentication itself
				.requestMatchers("/tracks").permitAll()
				// Permit all GET requests by default
				.requestMatchers(HttpMethod.GET, "/**").permitAll()
				//  Secure all POST requests by default
				.requestMatchers(HttpMethod.POST, "/**").authenticated()
				// Only allow get and post methods by default
				.anyRequest().denyAll()
				.and()
			.sessionManagement()
				.sessionFixation().changeSessionId()
				.and()
			
			.rememberMe()
				.key("remember")
				.tokenValiditySeconds(UtilImpl.REMEMBER_ME_TOKEN_TIMEOUT_HOURS * 60 * 60)
				.rememberMeParameter("remember")
				.rememberMeCookieName("remember")
				.tokenRepository(tokenRepository())
				.useSecureCookie(Util.isSecureUrl())
				.and()
			.formLogin()
				.defaultSuccessUrl(Util.getHomeUrl())
				.loginPage(Util.getLoginUrl())
				.loginProcessingUrl("/loginAttempt")
				.failureUrl(Util.getLoginUrl() + "?error")
				.successHandler(new SkyveAuthenticationSuccessHandler(userDetailsManager()))
				.and()
			.logout()
				.logoutSuccessUrl(Util.getLoggedOutUrl())
				.deleteCookies("JSESSIONID")
				.and()
			.csrf().disable()
			.httpBasic().disable()
			.headers()
				.httpStrictTransportSecurity().disable()
				.frameOptions().disable()
				.contentTypeOptions().disable();

		TwoFactorAuthPushEmailFilter tfaEmail = new TwoFactorAuthPushEmailFilter(userDetailsManager());
		http.addFilterBefore(tfaEmail, UsernamePasswordAuthenticationFilter.class);

		if ((UtilImpl.AUTHENTICATION_GOOGLE_CLIENT_ID != null)
				|| (UtilImpl.AUTHENTICATION_FACEBOOK_CLIENT_ID != null)
				|| (UtilImpl.AUTHENTICATION_GITHUB_CLIENT_ID != null)
				|| (UtilImpl.AUTHENTICATION_AZUREAD_TENANT_ID != null)) {
			http.oauth2Login()
					.defaultSuccessUrl(Util.getHomeUrl())
					.loginPage(Util.getLoginUrl())
					.failureUrl(Util.getLoginUrl() + "?error")
					.successHandler(new SkyveAuthenticationSuccessHandler(userDetailsManager()));
		}

//		http.saml2Login()
//				.defaultSuccessUrl(Util.getHomeUrl())
//				.loginPage(Util.getLoginUrl())
//				.failureUrl(Util.getLoginUrl() + "?error")
//				.successHandler(new SkyveAuthenticationSuccessHandler(userDetailsManager()));

		DefaultSecurityFilterChain result = http.build();
		// Note AuthenticationManager is not available as a shared object until after build()
		AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
		tfaEmail.setAuthenticationManager(authenticationManager);
		return result;
	}

/*
	@Bean
	public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
		RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
				.fromMetadataLocation("https://login.microsoftonline.com/blah-blah-blah")
				.registrationId("<app-name-registered-with-AD>")
				.entityId("https://{baseHost}/saml2/service-provider-metadata/{registrationId}")
				.assertionConsumerServiceLocation("https://{baseHost}/login/saml2/sso/{registrationId}")
				.build();
		return new InMemoryRelyingPartyRegistrationRepository(relyingPartyRegistration);
	}
*/
    
	@Bean
	public PasswordEncoder passwordEncoder() {
		return skyve.passwordEncoder();
	}
	
	@Bean
	public PersistentTokenRepository tokenRepository() throws Exception {
		return skyve.tokenRepository();
	}
	
	@Bean
	public UserDetailsManager userDetailsManager() {
		return skyve.jdbcUserDetailsManager();
	}
	
 	@Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
 		return skyve.clientRegistrationRepository();
    }

}
/*
    <!-- WAFFLE Configuration -->
<!--
	<http auto-config="false" use-expressions="true" entry-point-ref="negotiateSecurityFilterEntryPoint">
		<custom-filter ref="waffleNegotiateSecurityFilter" position="PRE_AUTH_FILTER" />
		...
    </http>
	<b:bean id="waffleWindowsAuthProvider" class="waffle.windows.auth.impl.WindowsAuthProviderImpl" />
	<b:bean id="negotiateSecurityFilterProvider" class="waffle.servlet.spi.NegotiateSecurityFilterProvider">
		<b:constructor-arg ref="waffleWindowsAuthProvider" />
	</b:bean>
	<b:bean id="basicSecurityFilterProvider" class="waffle.servlet.spi.BasicSecurityFilterProvider">
		<b:constructor-arg ref="waffleWindowsAuthProvider" />
	</b:bean>
	<b:bean id="waffleSecurityFilterProviderCollection" class="waffle.servlet.spi.SecurityFilterProviderCollection">
		<b:constructor-arg>
			<b:list>
				<b:ref bean="negotiateSecurityFilterProvider" />
			</b:list>
		</b:constructor-arg>
	</b:bean>
	<b:bean id="negotiateSecurityFilterEntryPoint" class="waffle.spring.NegotiateSecurityFilterEntryPoint">
		<b:property name="provider" ref="waffleSecurityFilterProviderCollection" />
	</b:bean>
	<b:bean id="waffleNegotiateSecurityFilter" class="waffle.spring.NegotiateSecurityFilter">
		<b:property name="provider" ref="waffleSecurityFilterProviderCollection" />
		<b:property name="allowGuestLogin" value="false" />
		<b:property name="principalFormat" value="fqn" />
		<b:property name="roleFormat" value="both" />
	</b:bean>
-->
	<!-- Single Customer Basic Auth -->
<!--
	<authentication-manager id="authenticationManager">
		...
	</authentication-manager>

	<http auto-config="false" use-expressions="true" entry-point-ref="skyveEntryPoint">
 		<custom-filter ref="skyveFilter" before="BASIC_AUTH_FILTER" />
 		...
 	</http>
	<b:bean id="skyveEntryPoint" class="org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint">
 		<b:property name="realmName" value="Skyve" />
 	</b:bean>
 	<b:bean id="skyveFilter" class="org.skyve.impl.web.spring.SingleCustomerBasicAuthenticationFilter">
 		<b:constructor-arg name="authenticationManager" ref="authenticationManager" />
 	</b:bean>
 -->
*/