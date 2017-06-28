package org.dogepool.practicalrx.services;

import java.util.Map;

import org.dogepool.practicalrx.error.DogePoolException;
import org.dogepool.practicalrx.error.Error;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Flux;

/**
 * A facade service to get DOGE to USD and DOGE to other currencies exchange
 * rates.
 */
@Service
public class ExchangeRateService {

	@Value("${doge.api.baseUrl}")
	private String dogeUrl;

	@Value("${exchange.free.api.baseUrl}")
	private String exchangeUrl;

	@Value("${exchange.nonfree.api.baseUrl}")
	private String exchangeNonfreeUrl;

	@Autowired
	private AdminService adminService;

	@Autowired
	private RestTemplate restTemplate;

	public Flux<Double> dogeToCurrencyExchangeRate(String targetCurrencyCode) {
		Flux<Double> dollarToCurrency = dollarToCurrency(targetCurrencyCode)
				.doOnError(e -> System.out.println("FALLING BACK TO NON-FREE EXCHANGE RATE SERVICE - " + e))
				.onErrorResume(t -> (t instanceof DogePoolException) ? dollarToCurrencyPaid(targetCurrencyCode)
						: Flux.error(t));

		return dogeToDollar().zipWith(dollarToCurrency, (doge2usd, usd2currency) -> doge2usd * usd2currency);
	}

	private Flux<Double> dogeToDollar() {
		return Flux.create(sub -> {
			try {
				Double rate = restTemplate.getForObject(dogeUrl, Double.class);
				sub.next(rate);
				sub.complete();
			} catch (RestClientException e) {
				sub.error(new DogePoolException("Unable to reach doge rate service at " + dogeUrl,
						Error.UNREACHABLE_SERVICE, HttpStatus.REQUEST_TIMEOUT));
			} catch (Exception e) {
				sub.error(e);
			}
		});
	}

	private Flux<Double> dollarToCurrency(String currencyCode) {
		return Flux.<Double>create(sub -> {
			try {
				Map result = restTemplate.getForObject(exchangeUrl + "/{from}/{to}", Map.class, "USD", currencyCode);
				Double rate = (Double) result.get("exchangeRate");
				if (rate == null)
					rate = (Double) result.get("rate");

				if (rate == null) {
					sub.error(new DogePoolException("Malformed exchange rate", Error.BAD_CURRENCY,
							HttpStatus.UNPROCESSABLE_ENTITY));
				}
				sub.next(rate);
				sub.complete();
			} catch (HttpStatusCodeException e) {
				sub.error(
						new DogePoolException("Error processing currency in free API : " + e.getResponseBodyAsString(),
								Error.BAD_CURRENCY, e.getStatusCode()));
			} catch (RestClientException e) {
				sub.error(new DogePoolException("Unable to reach free currency exchange service at " + exchangeUrl,
						Error.UNREACHABLE_SERVICE, HttpStatus.REQUEST_TIMEOUT));
			} catch (Exception e) {
				sub.error(e);
			}
		});
	}

	private Flux<Double> dollarToCurrencyPaid(String currencyCode) {
		return Flux.<Double>create(sub -> {
			try {
				Map result = restTemplate.getForObject(exchangeNonfreeUrl + "/{from}/{to}", Map.class, "USD",
						currencyCode);
				Double rate = (Double) result.get("exchangeRate");
				if (rate == null)
					rate = (Double) result.get("rate");

				if (rate == null) {
					sub.error(new DogePoolException("Malformed exchange rate from paid service", Error.BAD_CURRENCY,
							HttpStatus.UNPROCESSABLE_ENTITY));
				}
				sub.next(rate);
				sub.complete();
			} catch (HttpStatusCodeException e) {
				sub.error(
						new DogePoolException("Error processing currency in paid API : " + e.getResponseBodyAsString(),
								Error.BAD_CURRENCY, e.getStatusCode()));
			} catch (RestClientException e) {
				sub.error(new DogePoolException("Unable to reach paid currency exchange service at " + exchangeUrl,
						Error.UNREACHABLE_SERVICE, HttpStatus.REQUEST_TIMEOUT));
			}
		}).doOnNext(r -> adminService.addCost(1)).doOnComplete(
				() -> System.out.println("CALLED PAID EXCHANGE RATE SERVICE FOR 1$"));
	}

}
