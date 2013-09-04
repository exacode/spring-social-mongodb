package org.springframework.social.connect;

import java.util.List;
import java.util.Set;

import org.springframework.util.MultiValueMap;

/**
 * Data access layer that separates spring social logic from persistance layer.
 * <p>
 * Original spring supporting schema is defined in lib
 * {@code spring-social-core} in file
 * {@code /org/springframework/social/connect/jdbc/JdbcUsersConnectionRepository.sql}
 * .
 * 
 * @author mendlik
 * 
 */
public interface SocialConnectionDao {

	int getMaxRank(String userId, String providerId);

	void create(String userId, Connection<?> userConn, int rank);

	void update(String userId, Connection<?> userConn);

	void remove(String userId, ConnectionKey connectionKey);

	void remove(String userId, String providerId);

	Connection<?> getPrimaryConnection(String userId, String providerId);

	Connection<?> getConnection(String userId, String providerId,
			String providerUserId);

	List<Connection<?>> getConnections(String userId);

	List<Connection<?>> getConnections(String userId, String providerId);

	List<Connection<?>> getConnections(String userId,
			MultiValueMap<String, String> providerUsers);

	Set<String> getUserIds(String providerId, Set<String> providerUserIds);

	List<String> getUserIds(String providerId, String providerUserId);

}