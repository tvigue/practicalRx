package org.dogepool.practicalrx.services;

import org.dogepool.practicalrx.domain.User;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Service for getting info on coins mined by users.
 */
@Service
public class CoinService {

	public Flux<Long> totalCoinsMinedBy(User user) {
		if (user.equals(User.OTHERUSER)) {
			return Flux.just(12L);
		} else {
			return Flux.just(user.displayName).map(n -> n.length() / 2L);
		}
	}

}
