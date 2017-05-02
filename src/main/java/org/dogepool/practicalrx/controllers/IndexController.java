package org.dogepool.practicalrx.controllers;

import java.util.Map;

import org.dogepool.practicalrx.services.ExchangeRateService;
import org.dogepool.practicalrx.services.PoolRateService;
import org.dogepool.practicalrx.services.PoolService;
import org.dogepool.practicalrx.services.RankingService;
import org.dogepool.practicalrx.views.models.IndexModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import io.reactivex.Single;

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

	@RequestMapping("/")
	public Single<ModelAndView> index(Map<String, Object> model) {
		// prepare the error catching observable for currency rates
		Single<String> doge2usd = exchangeRateService.dogeToCurrencyExchangeRate("USD")
				.map(rate -> "1 DOGE = " + rate + "$")
				.onErrorReturn(e -> "1 DOGE = ??$, couldn't get the exchange rate - " + e)
				.single("?");
		Single<String> doge2eur = exchangeRateService.dogeToCurrencyExchangeRate("EUR")
				.map(rate -> "1 DOGE = " + rate + "€")
				.onErrorReturn(e -> "1 DOGE = ??€, couldn't get the exchange rate - " + e)
				.single("?");
		// prepare a model
		Single<IndexModel> modelZip = Single.zip(rankService.getLadderByHashrate().toList(),
				rankService.getLadderByCoins().toList(), poolService.miningUsers().count(),
				poolRateService.poolGigaHashrate().single(0D), doge2usd, doge2eur, (lh, lc, muc, pgr, d2u, d2e) -> {
					IndexModel idxModel = new IndexModel();
					idxModel.setPoolName(poolService.poolName());
					idxModel.setHashLadder(lh);
					idxModel.setCoinsLadder(lc);
					idxModel.setMiningUserCount(muc.intValue());
					idxModel.setGigaHashrate(pgr);
					idxModel.setDogeToUsdMessage(d2u);
					idxModel.setDogeToEurMessage(d2e);
					return idxModel;
				});

		// populate the model and call the template asynchronously
		return modelZip.map(idx -> {
			return new ModelAndView("index", "model", idx);
		});
	}

}
