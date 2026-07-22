package com.cbtpulsegrid.backend.identity.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.RefreshToken;
import com.cbtpulsegrid.backend.identity.RefreshTokenRepository;
import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.identity.UserStatus;
import com.cbtpulsegrid.backend.identity.security.JwtProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

	private static final int REFRESH_TOKEN_BYTES = 32;

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthenticationManager authenticationManager;
	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			AuthenticationManager authenticationManager,
			JwtEncoder jwtEncoder,
			JwtProperties jwtProperties
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	public TokenResponse login(String email, String password) {
		String normalizedEmail = normalizeEmail(email);
		try {
			authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, password)
			);
		}
		catch (AuthenticationException exception) {
			throw new BadCredentialsException("Invalid email or password", exception);
		}

		User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
				.filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
		return issueTokens(user, Instant.now());
	}

	public TokenResponse refresh(String rawRefreshToken) {
		Instant now = Instant.now();
		RefreshToken refreshToken = findValidRefreshToken(rawRefreshToken, now);
		User user = userRepository.findById(refreshToken.getUserId())
				.filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(InvalidRefreshTokenException::new);

		refreshToken.revoke(now);
		return issueTokens(user, now);
	}

	public void logout(String rawRefreshToken) {
		Instant now = Instant.now();
		RefreshToken refreshToken = findValidRefreshToken(rawRefreshToken, now);
		refreshToken.revoke(now);
	}

	private RefreshToken findValidRefreshToken(String rawRefreshToken, Instant now) {
		String tokenHash = sha256(rawRefreshToken);
		RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(InvalidRefreshTokenException::new);

		if (refreshToken.getRevokedAt() != null || !refreshToken.getExpiresAt().isAfter(now)) {
			throw new InvalidRefreshTokenException();
		}
		return refreshToken;
	}

	private TokenResponse issueTokens(User user, Instant issuedAt) {
		Instant accessTokenExpiresAt = issuedAt.plus(jwtProperties.accessTokenExpiry());
		String accessToken = createAccessToken(user, issuedAt, accessTokenExpiresAt);

		String rawRefreshToken = generateRefreshToken();
		Instant refreshTokenExpiresAt = issuedAt.plus(jwtProperties.refreshTokenExpiry());
		refreshTokenRepository.save(
				new RefreshToken(user.getId(), sha256(rawRefreshToken), refreshTokenExpiresAt)
		);

		return new TokenResponse(accessToken, rawRefreshToken, "Bearer", accessTokenExpiresAt);
	}

	private String createAccessToken(User user, Instant issuedAt, Instant expiresAt) {
		List<String> roles = user.getRoles().stream()
				.map(Role::name)
				.sorted()
				.toList();

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.subject(user.getId().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.id(UUID.randomUUID().toString())
				.claim("email", user.getEmail())
				.claim("roles", roles);

		if (user.getInstitutionId() != null) {
			claims.claim("institutionId", user.getInstitutionId().toString());
		}

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
				.type("JWT")
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
