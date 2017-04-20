package org.dogepool.practicalrx.controllers;

import java.util.List;

import org.dogepool.practicalrx.domain.User;
import org.dogepool.practicalrx.domain.UserStat;
import org.dogepool.practicalrx.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import rx.Single;

@RestController
@RequestMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class SearchController {

	@Autowired
	private SearchService service;

	@RequestMapping("user/{pattern}")
	public Single<List<User>> searchByName(@PathVariable String pattern) {
		return service.findByName(pattern).toList().toSingle();
	}

	@RequestMapping("user/coins/{minCoins}")
	public Single<List<UserStat>> searchByCoins(@PathVariable long minCoins) {
		return this.searchByCoins(minCoins, -1L);
	}

	@RequestMapping("user/coins/{minCoins}/{maxCoins}")
	private Single<List<UserStat>> searchByCoins(@PathVariable long minCoins, @PathVariable long maxCoins) {
		return service.findByCoins(minCoins, maxCoins).toList().toSingle();
	}

}
