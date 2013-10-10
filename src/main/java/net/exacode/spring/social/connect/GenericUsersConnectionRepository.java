package net.exacode.spring.social.connect;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UsersConnectionRepository;

/**
 * {@link UsersConnectionRepository} that abstracts from persistence layer.
 * 
 * @author mendlik
 */
public class GenericUsersConnectionRepository implements
		UsersConnectionRepository {

	private final SocialConnectionDao mongoService;

	private final ConnectionFactoryLocator connectionFactoryLocator;

	private ConnectionSignUp connectionSignUp;

	public GenericUsersConnectionRepository(SocialConnectionDao mongoService,
			ConnectionFactoryLocator connectionFactoryLocator) {
		this.mongoService = mongoService;
		this.connectionFactoryLocator = connectionFactoryLocator;
	}

	public void setConnectionSignUp(ConnectionSignUp connectionSignUp) {
		this.connectionSignUp = connectionSignUp;
	}

	@Override
	public List<String> findUserIdsWithConnection(Connection<?> connection) {
		ConnectionKey key = connection.getKey();
		List<String> localUserIds = mongoService.getUserIds(
				key.getProviderId(), key.getProviderUserId());
		if (localUserIds.size() == 0 && connectionSignUp != null) {
			String newUserId = connectionSignUp.execute(connection);
			if (newUserId != null) {
				createConnectionRepository(newUserId).addConnection(connection);
				return Arrays.asList(newUserId);
			}
		}
		return localUserIds;
	}

	@Override
	public Set<String> findUserIdsConnectedTo(String providerId,
			Set<String> providerUserIds) {

		return mongoService.getUserIds(providerId, providerUserIds);
	}

	@Override
	public ConnectionRepository createConnectionRepository(String userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId cannot be null");
		}
		return new GenericConnectionRepository(userId, mongoService,
				connectionFactoryLocator);
	}

}