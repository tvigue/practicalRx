package org.dogepool.practicalrx.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dogepool.practicalrx.domain.User;
import org.dogepool.practicalrx.domain.UserStat;
import org.dogepool.practicalrx.services.PoolRateService;
import org.dogepool.practicalrx.services.PoolService;
import org.dogepool.practicalrx.services.RankingService;
import org.dogepool.practicalrx.services.StatService;
import org.dogepool.practicalrx.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/pool", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoolController {

	@Autowired
	private UserService userService;

	@Autowired
	private RankingService rankingService;

	@Autowired
	private PoolService poolService;

	@Autowired
	private PoolRateService poolRateService;

	@Autowired
	private StatService statService;

	@RequestMapping("/ladder/hashrate")
	public Mono<List<UserStat>> ladderByHashrate() {
		return rankingService.getLadderByHashrate().collectList();
	}

	@RequestMapping("/ladder/coins")
	public Mono<List<UserStat>> ladderByCoins() {
		return rankingService.getLadderByCoins().collectList();
	}

	@RequestMapping("/hashrate")
	public Mono<Map<String, Object>> globalHashRate() {
		Map<String, Object> json = new HashMap<>(2);
		Mono<Double> ghashrate = poolRateService.poolGigaHashrate().single();
		return ghashrate.map(d -> {
			if (d < 1) {
				json.put("unit", "MHash/s");
				json.put("hashrate", d * 100d);
			} else {
				json.put("unit", "GHash/s");
				json.put("hashrate", d);
			}
			return json;
		});
	}

	@RequestMapping("/miners")
	public Mono<Map<String, Object>> miners() {
		Mono<Integer> allUsers = userService.findAll().count().map(l -> l.intValue());
		Mono<Integer> miningUsers = poolService.miningUsers().count().map(l -> l.intValue());
		Mono<Map<String, Object>> result = Mono.zip(args -> {
			Map<String, Object> json = new HashMap<>(2);
			json.put("totalUsers", (Integer) args[0]);
			json.put("totalMiningUsers", (Integer) args[1]);
			return json;
		}, allUsers, miningUsers);
		return result;
	}

	@RequestMapping("/miners/active")
	public Mono<List<User>> activeMiners() {
		return poolService.miningUsers().collectList();
	}

	@RequestMapping("/lastblock")
	public Mono<Map<String, Object>> lastBlock() {
		Mono<LocalDateTime> found = statService.lastBlockFoundDate().single(LocalDateTime.now());
		Mono<User> foundBy;

		try {
			foundBy = statService.lastBlockFoundBy().single(User.USER);
		} catch (IndexOutOfBoundsException e) {
			System.err.println("WARNING: StatService failed to return the last user to find a coin");
			foundBy = Mono.just(new User(-1, "BAD USER", "Bad User from StatService, please ignore", "", null));
		}

		Mono<Map<String, Object>> result = Mono.zip(args -> {
			Duration foundAgo = Duration.between((LocalDateTime) args[0], LocalDateTime.now());
			Map<String, Object> json = new HashMap<>(2);
			json.put("foundOn", ((LocalDateTime) args[0]).format(DateTimeFormatter.ISO_DATE_TIME));
			json.put("foundAgo", foundAgo.toMinutes());
			json.put("foundBy", (User) args[1]);
			return json;
		}, found, foundBy);

		return result;
	}

}
