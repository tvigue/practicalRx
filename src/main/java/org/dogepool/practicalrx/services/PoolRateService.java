package org.dogepool.practicalrx.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Service to retrieve the current global hashrate for the pool.
 */
@Service
public class PoolRateService {

	@Autowired
	private PoolService poolService;

	@Autowired
	private HashrateService hashrateService;

	public Flux<Double> poolGigaHashrate() {
		return poolService.miningUsers()
				.flatMap(u -> hashrateService.hashrateFor(u))
				.reduce(0d, (pools, users) -> pools + users)
				.flux();
	}

}
