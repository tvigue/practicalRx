package org.dogepool.practicalrx.services;

import org.dogepool.practicalrx.domain.User;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Service to retrieve hashrate information of users.
 */
@Service
public class HashrateService {

	/**
	 * @param user
	 * @return the last known gigahash/sec hashrate for the given user
	 */
	public Flux<Double> hashrateFor(User user) {
		if (user.equals(User.USER)) {
			return Flux.just(1.234);
		}
		return Flux.just(user.displayName).map(n -> n.length() / 100d);
	}

}
