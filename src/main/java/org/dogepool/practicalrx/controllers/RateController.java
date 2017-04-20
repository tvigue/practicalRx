package org.dogepool.practicalrx.controllers;

import org.dogepool.practicalrx.domain.ExchangeRate;
import org.dogepool.practicalrx.services.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import rx.Single;

@RestController
@RequestMapping(value = "/rate", produces = MediaType.APPLICATION_JSON_VALUE)
public class RateController {

	@Autowired
	private ExchangeRateService service;

	@RequestMapping("{moneyTo}")
	public Single<DeferredResult<ExchangeRate>> rate(@PathVariable String moneyTo) {
		DeferredResult<ExchangeRate> result = new DeferredResult<>();
		return service.dogeToCurrencyExchangeRate(moneyTo)
				.map(rate -> new ExchangeRate("DOGE", moneyTo, rate))
				.map(exchangeRate -> {
					result.setResult(exchangeRate);
					return result;
				})
				.doOnError(error -> result.setErrorResult(error))
				.toSingle();
	}

}
