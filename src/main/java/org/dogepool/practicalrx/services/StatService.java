package org.dogepool.practicalrx.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.dogepool.practicalrx.domain.User;
import org.dogepool.practicalrx.domain.UserStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Service to get stats on the pool, like top 10 ladders for various criteria.
 */
@Service
public class StatService {

	@Autowired
	private HashrateService hashrateService;

	@Autowired
	private CoinService coinService;

	@Autowired
	private UserService userService;

	public Flux<UserStat> getAllStats() {
		return userService.findAll().flatMap(u -> {
			Flux<Double> hr = hashrateService.hashrateFor(u);
			Flux<Long> co = coinService.totalCoinsMinedBy(u);

			return Flux.zip(hr, co, (rate, coin) -> new UserStat(u, rate, coin));
		});
	}

	public Flux<LocalDateTime> lastBlockFoundDate() {
		Random rng = new Random(System.currentTimeMillis());
		LocalDateTime date = LocalDateTime.now().minus(rng.nextInt(72), ChronoUnit.HOURS);
		return Flux.just(date);
	}

	public Flux<User> lastBlockFoundBy() {
		final Random rng = new Random(System.currentTimeMillis());
		return Flux.defer(() -> Flux.just(rng.nextInt(10)))
				.doOnNext(i -> System.out.println("ELECTED: #" + i))
				.flatMap(potentiallyBadIndex -> userService.findAll().elementAt(potentiallyBadIndex).flux())
				.retry();
	}

}
