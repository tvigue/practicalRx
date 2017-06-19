package org.dogepool.practicalrx.services;

import java.util.HashSet;
import java.util.Set;

import org.dogepool.practicalrx.domain.User;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Service to retrieve information on the current status of the mining pool
 */
@Service
public class PoolService {

	private final Set<User> connectedUsers = new HashSet<>();

	public String poolName() {
		return "Wow Such Pool!";
	}

	public Flux<User> miningUsers() {
		return Flux.fromIterable(connectedUsers);
	}

	public Flux<Boolean> connectUser(User user) {
		return Flux.<Boolean>create(s -> {
			connectedUsers.add(user);
			s.next(Boolean.TRUE);
			s.complete();
		}).doOnNext(b -> System.out.println(user.nickname + " connected"));
	}

	public Flux<Boolean> disconnectUser(User user) {
		return Flux.<Boolean>create(s -> {
			connectedUsers.remove(user);
			s.next(Boolean.TRUE);
			s.complete();
		}).doOnNext(b -> System.out.println(user.nickname + " disconnected"));
	}

}
