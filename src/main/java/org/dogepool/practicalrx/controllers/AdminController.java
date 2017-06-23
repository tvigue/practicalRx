package org.dogepool.practicalrx.controllers;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dogepool.practicalrx.domain.User;
import org.dogepool.practicalrx.error.DogePoolException;
import org.dogepool.practicalrx.error.Error;
import org.dogepool.practicalrx.services.AdminService;
import org.dogepool.practicalrx.services.PoolService;
import org.dogepool.practicalrx.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

	@Autowired
	private UserService userService;

	@Autowired
	private PoolService poolService;

	@Autowired
	private AdminService adminService;

	@RequestMapping(method = RequestMethod.POST, value = "/mining/{id}", consumes = MediaType.ALL_VALUE)
	public Mono<ResponseEntity<List<User>>> registerMiningUser(@PathVariable("id") long id) {
		return userService.getUser(id)
				.last()
				.onErrorResume(e -> Mono.error(new DogePoolException("User cannot mine, not authenticated",
						Error.BAD_USER, HttpStatus.NOT_FOUND)))
				.flatMapMany(u -> poolService.connectUser(u).single())
				.flatMap(b -> poolService.miningUsers().collectList())
				.map(l -> ResponseEntity.accepted().body(l))
				.single();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "mining/{id}", consumes = MediaType.ALL_VALUE)
	public Mono<ResponseEntity<List<User>>> deregisterMiningUser(@PathVariable("id") long id) {
		return userService.getUser(id)
				.last()
				.onErrorResume(e -> Mono.error(new DogePoolException("User isn't mining, not authenticated",
						Error.BAD_USER, HttpStatus.NOT_FOUND)))
				.flatMapMany(u -> poolService.disconnectUser(u).single())
				.flatMap(b -> poolService.miningUsers().collectList())
				.map(l -> ResponseEntity.accepted().body(l))
				.single();
	}

	@RequestMapping("/cost/{year}-{month}")
	public Mono<Map<String, Object>> cost(@PathVariable int year, @PathVariable int month) {
		Month monthEnum = Month.of(month);
		return cost(year, monthEnum);
	}

	@RequestMapping("/cost")
	public Mono<Map<String, Object>> cost() {
		LocalDate now = LocalDate.now();
		return cost(now.getYear(), now.getMonth());
	}

	@RequestMapping("/cost/{year}/{month}")
	protected Mono<Map<String, Object>> cost(@PathVariable int year, @PathVariable Month month) {
		return adminService.costForMonth(year, month).map(cost -> {
			Map<String, Object> json = new HashMap<>();
			json.put("month", month + " " + year);
			json.put("cost", cost);
			json.put("currency", "USD");
			json.put("currencySign", "$");
			return json;
		}).single();
	}

}
