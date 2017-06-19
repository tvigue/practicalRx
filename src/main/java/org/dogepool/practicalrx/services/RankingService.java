package org.dogepool.practicalrx.services;

import org.dogepool.practicalrx.domain.User;
import org.dogepool.practicalrx.domain.UserStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Service to get ladders and find a user's rankings in the pool.
 */
@Service
public class RankingService {

	@Autowired
	private StatService statService;

	/**
	 * Find the user's rank by hashrate in the pool. This is a costly operation.
	 * 
	 * @return the rank of the user in terms of hashrate. If it couldnt' be
	 *         established it'll be ranked last.
	 */
	public Flux<Integer> rankByHashrate(User user) {
		return rankByHashrate().takeUntil(stat -> stat.user.equals(user))
				.count()
				.map(l -> l.intValue())
				.flux();
	}

	/**
	 * Find the user's rank by number of coins found. This is a costly
	 * operation.
	 * 
	 * @return the rank of the user in terms of coins found. If user is not
	 *         found, it will be ranked last.
	 */
	public Flux<Integer> rankByCoins(User user) {
		return rankByCoins().takeUntil(userStat -> user.equals(userStat.user))
				.count()
				.map(l -> l.intValue())
				.flux();
	}

	public Flux<UserStat> getLadderByHashrate() {
		return rankByHashrate().take(10);
	}

	public Flux<UserStat> getLadderByCoins() {
		return rankByCoins().take(10);
	}

	protected Flux<UserStat> rankByHashrate() {
		return statService.getAllStats().sort((o1, o2) -> {
			double h1 = o1.hashrate;
			double h2 = o2.hashrate;
			double diff = h2 - h1;
			if (diff == 0d) {
				return 0;
			} else {
				return diff > 0d ? 1 : -1;
			}
		});
	}

	protected Flux<UserStat> rankByCoins() {
		return statService.getAllStats().sort((o1, o2) -> {
			long c1 = o1.totalCoinsMined;
			long c2 = o2.totalCoinsMined;
			long diff = c2 - c1;
			if (diff == 0L) {
				return 0;
			} else {
				return diff > 0L ? 1 : -1;
			}
		});
	}

}
