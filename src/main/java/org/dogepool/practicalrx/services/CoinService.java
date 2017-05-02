package org.dogepool.practicalrx.services;

import org.dogepool.practicalrx.domain.User;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

/**
 * Service for getting info on coins mined by users.
 */
@Service
public class CoinService {

	public Observable<Long> totalCoinsMinedBy(User user) {
		if (user.equals(User.OTHERUSER)) {
			return Observable.just(12L);
		} else {
			return Observable.just(user.displayName).map(n -> n.length() / 2L);
		}
	}

}
