package org.dogepool.practicalrx.controllers;

import org.dogepool.practicalrx.domain.ExchangeRate;
import org.dogepool.practicalrx.services.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/rate", produces = MediaType.APPLICATION_JSON_VALUE)
public class RateController {

	@Autowired
	private ExchangeRateService service;

	@RequestMapping("{moneyTo}")
	public Mono<ExchangeRate> rate(@PathVariable String moneyTo) {
		return service.dogeToCurrencyExchangeRate(moneyTo)
				.map(rate -> new ExchangeRate("DOGE", moneyTo, rate))
				.single(new ExchangeRate("", "", 0D));
	}

}
