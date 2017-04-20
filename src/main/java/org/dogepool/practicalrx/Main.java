package org.dogepool.practicalrx;

import java.io.File;
import java.util.List;

import org.dogepool.practicalrx.domain.User;
import org.dogepool.practicalrx.domain.UserStat;
import org.dogepool.practicalrx.services.ExchangeRateService;
import org.dogepool.practicalrx.services.PoolRateService;
import org.dogepool.practicalrx.services.PoolService;
import org.dogepool.practicalrx.services.RankingService;
import org.dogepool.practicalrx.services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		checkConfig();
		SpringApplication.run(Main.class, args);
	}

	private static void checkConfig() {
		File mainConfig = new File("src/main/resources/application.properties");
		File testConfig = new File("src/test/resources/application.properties");

		System.out.println(mainConfig.isFile() + " " + testConfig.isFile());

		if (!mainConfig.isFile() || !testConfig.isFile()) {
			throw new IllegalStateException(
					"\n\n========PLEASE CONFIGURE PROJECT========" + "\nApplication configuration not found, have you:"
							+ "\n\t - copied \"application.main\" to \"src/main/resources/application.properties\"?"
							+ "\n\t - copied \"application.test\" to \"src/test/resources/application.properties\"?"
							+ "\n\t - edited these files with the correct configuration?"
							+ "\n========================================\n");
		}
	}

	@Bean
	@ConditionalOnBean(value = Bucket.class)
	@Order(value = 1)
	CommandLineRunner userCreation(Bucket couchbaseBucket) {
		return args -> {
			JsonDocument u1 = JsonDocument.create(String.valueOf(User.USER.id), User.USER.toJsonObject());
			JsonDocument u2 = JsonDocument.create(String.valueOf(User.OTHERUSER.id), User.OTHERUSER.toJsonObject());
			couchbaseBucket.upsert(u1);
			couchbaseBucket.upsert(u2);
		};
	}

	@Bean
	@Order(value = 2)
	CommandLineRunner commandLineRunner(UserService userService, RankingService rankinService, PoolService poolService,
			PoolRateService poolRateService, ExchangeRateService exchangeRateService) {
		return args -> {
			// connect USER automatically
			userService.getUser(0).flatMap(u -> poolService.connectUser(u)).toBlocking().first();

			// gather data
			List<UserStat> hashLadder = rankinService.getLadderByHashrate().toList().toBlocking().first();
			List<UserStat> coinsLadder = rankinService.getLadderByCoins().toList().toBlocking().first();
			String poolName = poolService.poolName();
			int miningUserCount = poolService.miningUsers().count().toBlocking().first();
			double poolRate = poolRateService.poolGigaHashrate().toBlocking().first();

			// display welcome screen in console
			System.out.println("Welcome to " + poolName + " dogecoin mining pool!");
			System.out.println(
					miningUserCount + " users currently mining, for a global hashrate of " + poolRate + " GHash/s");

			exchangeRateService.dogeToCurrencyExchangeRate("USD").subscribe(
					r -> System.out.println("1 DOGE = " + r + "$"),
					e -> System.out.println("1 DOGE = ??$, couldn't get the exchange rate - " + e));
			exchangeRateService.dogeToCurrencyExchangeRate("EUR").subscribe(
					r -> System.out.println("1 DOGE = " + r + "€"),
					e -> System.out.println("1 DOGE = ??€, couldn't get the exchange rate - " + e));

			System.out.println("\n----- TOP 10 Miners by Hashrate -----");
			int count = 1;
			for (UserStat userStat : hashLadder) {
				System.out.println(count++ + ": " + userStat.user.nickname + ", " + userStat.hashrate + " GHash/s");
			}

			System.out.println("\n----- TOP 10 Miners by Coins Found -----");
			count = 1;
			for (UserStat userStat : coinsLadder) {
				System.out.println(
						count++ + ": " + userStat.user.nickname + ", " + userStat.totalCoinsMined + " dogecoins");
			}
		};
	}

}
