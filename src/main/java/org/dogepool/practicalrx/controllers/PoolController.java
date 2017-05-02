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

import io.reactivex.Observable;
import io.reactivex.Single;

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
	public Single<List<UserStat>> ladderByHashrate() {
		return rankingService.getLadderByHashrate().toList();
	}

	@RequestMapping("/ladder/coins")
	public Single<List<UserStat>> ladderByCoins() {
		return rankingService.getLadderByCoins().toList();
	}

	@RequestMapping("/hashrate")
	public Single<Map<String, Object>> globalHashRate() {
		Map<String, Object> json = new HashMap<>(2);
		Single<Double> ghashrate = poolRateService.poolGigaHashrate().single(0D);
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
	public Single<Map<String, Object>> miners() {
		Single<Integer> allUsers = userService.findAll().count().map(l -> l.intValue());
		Single<Integer> miningUsers = poolService.miningUsers().count().map(l -> l.intValue());
		Single<Map<String, Object>> result = Single.zip(allUsers, miningUsers, (allU, miningU) -> {
			Map<String, Object> json = new HashMap<>(2);
			json.put("totalUsers", allU);
			json.put("totalMiningUsers", miningU);
			return json;
		});
		return result;
	}

	@RequestMapping("/miners/active")
	public Single<List<User>> activeMiners() {
		return poolService.miningUsers().toList();
	}

	@RequestMapping("/lastblock")
	public Single<Map<String, Object>> lastBlock() {
		Single<LocalDateTime> found = statService.lastBlockFoundDate().single(LocalDateTime.now());
		Single<User> foundBy;

		try {
			foundBy = statService.lastBlockFoundBy().single(User.USER);
		} catch (IndexOutOfBoundsException e) {
			System.err.println("WARNING: StatService failed to return the last user to find a coin");
			foundBy = Single.just(new User(-1, "BAD USER", "Bad User from StatService, please ignore", "", null));
		}

		Single<Map<String, Object>> result = Single.zip(found, foundBy, (f, fB) -> {
			Duration foundAgo = Duration.between(f, LocalDateTime.now());
			Map<String, Object> json = new HashMap<>(2);
			json.put("foundOn", f.format(DateTimeFormatter.ISO_DATE_TIME));
			json.put("foundAgo", foundAgo.toMinutes());
			json.put("foundBy", fB);
			return json;
		});

		return result;
	}

}
