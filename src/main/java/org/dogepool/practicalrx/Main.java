package org.dogepool.practicalrx;

import java.io.File;

import org.dogepool.practicalrx.domain.User;
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

	private static int count = 1;

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
			JsonDocument uo = JsonDocument.create(String.valueOf(User.OTHERUSER.id), User.OTHERUSER.toJsonObject());
			couchbaseBucket.upsert(u1);
			couchbaseBucket.upsert(uo);
		};
	}

	@Bean
	@Order(value = 2)
	CommandLineRunner commandLineRunner(UserService userService, RankingService rankinService, PoolService poolService,
			PoolRateService poolRateService, ExchangeRateService exchangeRateService) {
		return args -> {
			// connect USER automatically
			userService.getUser(0).flatMap(u -> poolService.connectUser(u)).subscribe();

			// gather data
			String poolName = poolService.poolName();

			// display welcome screen in console
			System.out.println("Welcome to " + poolName + " dogecoin mining pool!");
			poolService.miningUsers().count().subscribe(miningUserCount -> System.out.print(miningUserCount),
					err -> System.out.print(err));
			System.out.print(" users currently mining, for a global hashrate of ");
			poolRateService.poolGigaHashrate().subscribe(poolRate -> System.out.print(poolRate),
					err -> System.out.print(err));
			System.out.println(" GHash/s");

			exchangeRateService.dogeToCurrencyExchangeRate("USD").subscribe(
					r -> System.out.println("1 DOGE = " + r + "$"),
					e -> System.out.println("1 DOGE = ??$, couldn't get the exchange rate - " + e));
			exchangeRateService.dogeToCurrencyExchangeRate("EUR").subscribe(
					r -> System.out.println("1 DOGE = " + r + "€"),
					e -> System.out.println("1 DOGE = ??€, couldn't get the exchange rate - " + e));

			System.out.println("\n----- TOP 10 Miners by Hashrate -----");
			rankinService.getLadderByHashrate().toList().subscribe(hashLadder -> {
				hashLadder.forEach(userStat -> System.out
						.println(count++ + ": " + userStat.user.nickname + ", " + userStat.hashrate + " GHash/s"));
			}, err -> System.out.println(count++ + ": ?, ? GHash/s\terror: " + err));

			System.out.println("\n----- TOP 10 Miners by Coins Found -----");
			count = 1;
			rankinService.getLadderByCoins().subscribe(
					userStat -> System.out.println(
							count++ + ": " + userStat.user.nickname + ", " + userStat.totalCoinsMined + " dogecoins"),
					err -> System.out.println(count++ + ": ?, ? dogecoins\terror: " + err));
		};
	}

}
