package net.exacode.spring.social.connect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.DuplicateConnectionException;
import org.springframework.social.connect.NoSuchConnectionException;
import org.springframework.social.connect.NotConnectedException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ConnectionRepository} that abstracts from persistence layer.
 * 
 * @author mendlik
 * 
 */
public class GenericConnectionRepository implements ConnectionRepository {

	private final String userId;

	private final SocialConnectionDao connectionService;

	private final ConnectionFactoryLocator connectionFactoryLocator;

	public GenericConnectionRepository(String userId,
			SocialConnectionDao connectionService,
			ConnectionFactoryLocator connectionFactoryLocator) {
		this.userId = userId;
		this.connectionService = connectionService;
		this.connectionFactoryLocator = connectionFactoryLocator;
	}

	/**
	 * Add a new connection to this repository for the current user.
	 */
	@Override
	public void addConnection(Connection<?> connection) {
		try {
			ConnectionData data = connection.createData();

			int rank = connectionService.getMaxRank(userId,
					data.getProviderId());
			connectionService.create(userId, connection, rank);

		} catch (DuplicateKeyException e) {
			throw new DuplicateConnectionException(connection.getKey());
		}
	}

	/**
	 * Find all connections the current user has across all providers
	 */
	@Override
	public MultiValueMap<String, Connection<?>> findAllConnections() {
		List<Connection<?>> resultList = connectionService
				.getConnections(this.userId);

		MultiValueMap<String, Connection<?>> connections = new LinkedMultiValueMap<String, Connection<?>>();
		Set<String> registeredProviderIds = this.connectionFactoryLocator
				.registeredProviderIds();
		for (String registeredProviderId : registeredProviderIds) {
			connections.put(registeredProviderId,
					Collections.<Connection<?>> emptyList());
		}

		for (Connection<?> connection : resultList) {
			String providerId = connection.getKey().getProviderId();
			if (connections.get(providerId).size() == 0) {
				connections.put(providerId, new LinkedList<Connection<?>>());
			}
			connections.add(providerId, connection);
		}
		return connections;
	}

	/**
	 * Find the connections the current user has to the provider of the given
	 * API
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <A> List<Connection<A>> findConnections(Class<A> apiType) {
		List<?> connections = findConnections(getProviderId(apiType));
		return (List<Connection<A>>) connections;
	}

	/**
	 * Find the connections the current user has to the provider registered by
	 * the given id
	 */
	@Override
	public List<Connection<?>> findConnections(String providerId) {
		return connectionService.getConnections(this.userId, providerId);
	}

	/**
	 * Find the connections the current user has to the given provider users.
	 */
	@Override
	public MultiValueMap<String, Connection<?>> findConnectionsToUsers(
			MultiValueMap<String, String> providerUsers) {
		if (providerUsers == null || providerUsers.isEmpty()) {
			throw new IllegalArgumentException(
					"Unable to execute find: no providerUsers provided");
		}

		List<Connection<?>> resultList = connectionService.getConnections(
				userId, providerUsers);

		MultiValueMap<String, Connection<?>> connectionsForUsers = new LinkedMultiValueMap<String, Connection<?>>();
		for (Connection<?> connection : resultList) {
			String providerId = connection.getKey().getProviderId();
			List<String> userIds = providerUsers.get(providerId);
			List<Connection<?>> connections = connectionsForUsers
					.get(providerId);
			if (connections == null) {
				connections = new ArrayList<Connection<?>>(userIds.size());
				for (int i = 0; i < userIds.size(); i++) {
					connections.add(null);
				}
				connectionsForUsers.put(providerId, connections);
			}
			String providerUserId = connection.getKey().getProviderUserId();
			int connectionIndex = userIds.indexOf(providerUserId);
			connections.set(connectionIndex, connection);
		}
		return connectionsForUsers;
	}

	/**
	 * Get a connection for the current user by its key
	 */
	@Override
	public Connection<?> getConnection(ConnectionKey connectionKey) {
		try {
			return connectionService.getConnection(userId,
					connectionKey.getProviderId(),
					connectionKey.getProviderUserId());
		} catch (EmptyResultDataAccessException e) {
			throw new NoSuchConnectionException(connectionKey);
		}
	}

	/**
	 * Get a connection between the current user and the given provider user.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <A> Connection<A> getConnection(Class<A> apiType,
			String providerUserId) {
		String providerId = getProviderId(apiType);
		return (Connection<A>) getConnection(new ConnectionKey(providerId,
				providerUserId));
	}

	/**
	 * Get the "primary" connection the current user has to the provider of the
	 * given API.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <A> Connection<A> getPrimaryConnection(Class<A> apiType) {
		String providerId = getProviderId(apiType);
		Connection<A> connection = (Connection<A>) findPrimaryConnection(providerId);
		if (connection == null) {
			throw new NotConnectedException(providerId);
		}
		return connection;
	}

	/**
	 * Find the "primary" connection the current user has to the provider of the
	 * given API
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <A> Connection<A> findPrimaryConnection(Class<A> apiType) {
		String providerId = getProviderId(apiType);
		return (Connection<A>) findPrimaryConnection(providerId);
	}

	/**
	 * Update a Connection already added to this repository.
	 */
	@Override
	public void updateConnection(Connection<?> connection) {
		connectionService.update(userId, connection);
	}

	/**
	 * Remove all Connections between the current user and the provider from
	 * this repository.
	 */
	@Override
	public void removeConnections(String providerId) {
		connectionService.remove(userId, providerId);
	}

	/**
	 * Remove a single Connection for the current user from this repository.
	 */
	@Override
	public void removeConnection(ConnectionKey connectionKey) {
		connectionService.remove(userId, connectionKey);
	}

	private <A> String getProviderId(Class<A> apiType) {
		return connectionFactoryLocator.getConnectionFactory(apiType)
				.getProviderId();
	}

	private Connection<?> findPrimaryConnection(String providerId) {
		// where userId = ? and providerId = ? and rank = 1
		return connectionService.getPrimaryConnection(userId, providerId);
	}
}