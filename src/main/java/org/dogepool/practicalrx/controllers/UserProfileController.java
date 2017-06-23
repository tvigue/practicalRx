package org.dogepool.practicalrx.controllers;

import java.util.Map;

import org.dogepool.practicalrx.domain.UserProfile;
import org.dogepool.practicalrx.error.DogePoolException;
import org.dogepool.practicalrx.error.Error;
import org.dogepool.practicalrx.services.CoinService;
import org.dogepool.practicalrx.services.HashrateService;
import org.dogepool.practicalrx.services.RankingService;
import org.dogepool.practicalrx.services.UserService;
import org.dogepool.practicalrx.views.models.MinerModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller(value = "/miner")
public class UserProfileController {

	@Value(value = "${avatar.api.baseUrl}")
	private String avatarBaseUrl;

	@Autowired
	private UserService userService;

	@Autowired
	private RankingService rankingService;

	@Autowired
	private HashrateService hashrateService;

	@Autowired
	private CoinService coinService;

	@Autowired
	private RestTemplate restTemplate;

	@RequestMapping(value = "/miner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<UserProfile> profile(@PathVariable int id) {
		return userService.getUser(id)
				.single()
				.onErrorResume(e -> Mono
						.error(new DogePoolException("Unknown miner " + e, Error.UNKNOWN_USER, HttpStatus.NOT_FOUND)))
				// find the avatar's url
				.flatMapMany(user -> {
					ResponseEntity<Map> avatarResponse = restTemplate.getForEntity(avatarBaseUrl + "/" + user.avatarId,
							Map.class);
					if (avatarResponse.getStatusCode().is2xxSuccessful()) {
						Map<String, ?> avatarInfo = avatarResponse.getBody();
						String avatarUrl = (String) avatarInfo.get("large");
						String smallAvatarUrl = (String) avatarInfo.get("small");

						// complete with other information
						return Mono.zip(
								// return the full profile
								args -> new UserProfile(user, (Double) args[0], (Long) args[1], avatarUrl,
										smallAvatarUrl, (Integer) args[2], (Integer) args[3]),
								hashrateService.hashrateFor(user).single(),
								coinService.totalCoinsMinedBy(user).single(),
								rankingService.rankByHashrate(user).single(),
								rankingService.rankByCoins(user).single());
					} else {
						return Mono.error(new DogePoolException("Unable to get avatar info", Error.UNREACHABLE_SERVICE,
								avatarResponse.getStatusCode()));
					}
				})
				.subscribeOn(Schedulers.elastic())
				.single();
	}

	@RequestMapping(value = "/miner/{id}", produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> miner(Map<String, Object> model, @PathVariable int id) {
		return userService.getUser(id)
				.single()
				.onErrorResume(e -> Mono
						.error(new DogePoolException("Unknown miner " + e, Error.UNKNOWN_USER, HttpStatus.NOT_FOUND)))
				// find the avatar's url
				.flatMapMany(user -> {
					ResponseEntity<Map> avatarResponse = restTemplate.getForEntity(avatarBaseUrl + "/" + user.avatarId,
							Map.class);
					if (avatarResponse.getStatusCode().is2xxSuccessful()) {
						Map<String, ?> avatarInfo = avatarResponse.getBody();
						String avatarUrl = (String) avatarInfo.get("large");
						String smallAvatarUrl = (String) avatarInfo.get("small");

						// complete with other information
						return Mono.zip(
								// create a model for the view
								args -> {
									// (hash, coins, rankByHash, rankByCoins)
									MinerModel minerModel = new MinerModel();
									minerModel.setAvatarUrl(avatarUrl);
									minerModel.setSmallAvatarUrl(smallAvatarUrl);
									minerModel.setBio(user.bio);
									minerModel.setDisplayName(user.displayName);
									minerModel.setNickname(user.nickname);
									minerModel.setRankByHash((Integer) args[2]);
									minerModel.setRankByCoins((Integer) args[3]);
									return minerModel;
								}, hashrateService.hashrateFor(user).single(),
								coinService.totalCoinsMinedBy(user).single(),
								rankingService.rankByHashrate(user).single(),
								rankingService.rankByCoins(user).single());
					} else {
						return Mono.error(new DogePoolException("Unable to get avatar info", Error.UNREACHABLE_SERVICE,
								avatarResponse.getStatusCode()));
					}
				})
				.subscribeOn(Schedulers.elastic())
				.map(minerModel -> {
					model.put("minerModel", minerModel);
					return "miner";
				})
				.single();
	}

}
