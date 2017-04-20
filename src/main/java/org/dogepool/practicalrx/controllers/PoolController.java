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

import rx.Observable;
import rx.Single;

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
		return rankingService.getLadderByHashrate().toList().toSingle();
	}

	@RequestMapping("/ladder/coins")
	public Single<List<UserStat>> ladderByCoins() {
		return rankingService.getLadderByCoins().toList().toSingle();
	}

	@RequestMapping("/hashrate")
	public Single<Map<String, Object>> globalHashRate() {
		Map<String, Object> json = new HashMap<>(2);
		Observable<Double> ghashrate = poolRateService.poolGigaHashrate();
		return ghashrate.map(d -> {
			if (d < 1) {
				json.put("unit", "MHash/s");
				json.put("hashrate", d * 100d);
			} else {
				json.put("unit", "GHash/s");
				json.put("hashrate", d);
			}
			return json;
		}).toSingle();
	}

	@RequestMapping("/miners")
	public Single<Map<String, Object>> miners() {
		Observable<Integer> allUsers = userService.findAll().count();
		Observable<Integer> miningUsers = poolService.miningUsers().count();
		Observable<Map<String, Object>> result = Observable.zip(allUsers, miningUsers, (allU, miningU) -> {
			Map<String, Object> json = new HashMap<>(2);
			json.put("totalUsers", allU);
			json.put("totalMiningUsers", miningU);
			return json;
		});
		return result.toSingle();
	}

	@RequestMapping("/miners/active")
	public Single<List<User>> activeMiners() {
		return poolService.miningUsers().toList().toSingle();
	}

	@RequestMapping("/lastblock")
	public Single<Map<String, Object>> lastBlock() {
		Observable<LocalDateTime> found = statService.lastBlockFoundDate();
		Observable<User> foundBy;

		try {
			foundBy = statService.lastBlockFoundBy();
		} catch (IndexOutOfBoundsException e) {
			System.err.println("WARNING: StatService failed to return the last user to find a coin");
			foundBy = Observable.just(new User(-1, "BAD USER", "Bad User from StatService, please ignore", "", null));
		}

		Observable<Map<String, Object>> result = Observable.zip(found, foundBy, (f, fB) -> {
			Duration foundAgo = Duration.between(f, LocalDateTime.now());
			Map<String, Object> json = new HashMap<>(2);
			json.put("foundOn", f.format(DateTimeFormatter.ISO_DATE_TIME));
			json.put("foundAgo", foundAgo.toMinutes());
			json.put("foundBy", fB);
			return json;
		});

		return result.toSingle();
	}

}
