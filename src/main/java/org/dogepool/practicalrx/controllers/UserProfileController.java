package org.dogepool.practicalrx.controllers;

import java.util.Map;

import org.dogepool.practicalrx.domain.User;
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

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

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
	public Single<UserProfile> profile(@PathVariable int id) {
		return userService.getUser(id)
				.singleOrError()
				.onErrorResumeNext(
						Single.error(new DogePoolException("Unknown miner", Error.UNKNOWN_USER, HttpStatus.NOT_FOUND)))
				// find the avatar's url
				.flatMap(user -> {
					ResponseEntity<Map> avatarResponse = restTemplate.getForEntity(avatarBaseUrl + "/" + user.avatarId,
							Map.class);
					if (avatarResponse.getStatusCode().is2xxSuccessful()) {
						Map<String, ?> avatarInfo = avatarResponse.getBody();
						String avatarUrl = (String) avatarInfo.get("large");
						String smallAvatarUrl = (String) avatarInfo.get("small");

						// complete with other information
						return Single.zip(hashrateService.hashrateFor(user).single(0D),
								coinService.totalCoinsMinedBy(user).single(0L),
								rankingService.rankByHashrate(user).single(0),
								rankingService.rankByCoins(user).single(0),
								// return the full profile
								(hash, coins, rankByHash, rankByCoins) -> new UserProfile(user, hash, coins, avatarUrl,
										smallAvatarUrl, rankByHash, rankByCoins));
					} else {
						return Single.error(new DogePoolException("Unable to get avatar info",
								Error.UNREACHABLE_SERVICE, avatarResponse.getStatusCode()));
					}
				})
				.subscribeOn(Schedulers.io());
	}

	@RequestMapping(value = "/miner/{id}", produces = MediaType.TEXT_HTML_VALUE)
	public Single<String> miner(Map<String, Object> model, @PathVariable int id) {
		return userService.getUser(id)
				.singleOrError()
				.onErrorResumeNext(
						Single.error(new DogePoolException("Unknown miner", Error.UNKNOWN_USER, HttpStatus.NOT_FOUND)))
				// find the avatar's url
				.flatMap(user -> {
					ResponseEntity<Map> avatarResponse = restTemplate.getForEntity(avatarBaseUrl + "/" + user.avatarId,
							Map.class);
					if (avatarResponse.getStatusCode().is2xxSuccessful()) {
						Map<String, ?> avatarInfo = avatarResponse.getBody();
						String avatarUrl = (String) avatarInfo.get("large");
						String smallAvatarUrl = (String) avatarInfo.get("small");

						// complete with other information
						return Single.zip(hashrateService.hashrateFor(user).single(0D),
								coinService.totalCoinsMinedBy(user).single(0L),
								rankingService.rankByHashrate(user).single(0),
								rankingService.rankByCoins(user).single(0),
								// create a model for the view
								(hash, coins, rankByHash, rankByCoins) -> {
									MinerModel minerModel = new MinerModel();
									minerModel.setAvatarUrl(avatarUrl);
									minerModel.setSmallAvatarUrl(smallAvatarUrl);
									minerModel.setBio(user.bio);
									minerModel.setDisplayName(user.displayName);
									minerModel.setNickname(user.nickname);
									minerModel.setRankByCoins(rankByCoins);
									minerModel.setRankByHash(rankByHash);
									return minerModel;
								});
					} else {
						return Single.error(new DogePoolException("Unable to get avatar info",
								Error.UNREACHABLE_SERVICE, avatarResponse.getStatusCode()));
					}
				})
				.subscribeOn(Schedulers.io())
				.map(minerModel -> {
					model.put("minerModel", minerModel);
					return "miner";
				});
	}

}
