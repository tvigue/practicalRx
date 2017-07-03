package org.dogepool.practicalrx.controllers;

import java.util.List;
import java.util.Map;

import org.dogepool.practicalrx.domain.UserStat;
import org.dogepool.practicalrx.services.ExchangeRateService;
import org.dogepool.practicalrx.services.PoolRateService;
import org.dogepool.practicalrx.services.PoolService;
import org.dogepool.practicalrx.services.RankingService;
import org.dogepool.practicalrx.views.models.IndexModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import reactor.core.publisher.Mono;
import rx.plugins.RxJavaHooks;

/**
 * A utility controller that displays the welcome message as HTML on root
 * endpoint.
 */
@Controller
public class IndexController {

	@Autowired
	private RankingService rankService;

	@Autowired
	private PoolService poolService;

	@Autowired
	private PoolRateService poolRateService;

	@Autowired
	private ExchangeRateService exchangeRateService;

	private static final String PATH = "/error";

	@RequestMapping("/")
	public Mono<ModelAndView> index(Map<String, Object> model) {

		RxJavaHooks.enableAssemblyTracking();

		// prepare the error catching observable for currency rates
		Mono<String> doge2usd = exchangeRateService.dogeToCurrencyExchangeRate("USD")
				.map(rate -> "1 DOGE = " + rate + "$")
				.onErrorReturn("1 DOGE = ??$, couldn't get the exchange rate - ")
				.single();
		Mono<String> doge2eur = exchangeRateService.dogeToCurrencyExchangeRate("EUR")
				.map(rate -> "1 DOGE = " + rate + "€")
				.onErrorReturn("1 DOGE = ??€, couldn't get the exchange rate - ")
				.single();
		// prepare a model
		Mono<IndexModel> modelZip = Mono.zip(args -> {
			IndexModel idxModel = new IndexModel();
			idxModel.setPoolName(poolService.poolName());
			idxModel.setHashLadder((List<UserStat>) args[0]);
			idxModel.setCoinsLadder((List<UserStat>) args[1]);
			idxModel.setMiningUserCount(((Long) args[2]).intValue());
			idxModel.setGigaHashrate((Double) args[3]);
			idxModel.setDogeToUsdMessage((String) args[4]);
			idxModel.setDogeToEurMessage((String) args[5]);
			return idxModel;
		}, rankService.getLadderByHashrate().collectList(), rankService.getLadderByCoins().collectList(),
				poolService.miningUsers().count(), poolRateService.poolGigaHashrate().single(), doge2usd, doge2eur);

		model.put("test", "test");
		
		// populate the model and call the template asynchronously
		return modelZip.map(idx -> {
			return new ModelAndView("index", "model", idx);
		});
	}

}
