package org.dogepool.practicalrx.services;

import java.util.HashSet;
import java.util.Set;

import org.dogepool.practicalrx.domain.User;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

/**
 * Service to retrieve information on the current status of the mining pool
 */
@Service
public class PoolService {

	private final Set<User> connectedUsers = new HashSet<>();

	public String poolName() {
		return "Wow Such Pool!";
	}

	public Observable<User> miningUsers() {
		return Observable.fromIterable(connectedUsers);
	}

	public Observable<Boolean> connectUser(User user) {
		return Observable.<Boolean>create(s -> {
			connectedUsers.add(user);
			s.onNext(Boolean.TRUE);
			s.onComplete();
		}).doOnNext(b -> System.out.println(user.nickname + " connected"));
	}

	public Observable<Boolean> disconnectUser(User user) {
		return Observable.<Boolean>create(s -> {
			connectedUsers.remove(user);
			s.onNext(Boolean.TRUE);
			s.onComplete();
		}).doOnNext(b -> System.out.println(user.nickname + " disconnected"));
	}

}
