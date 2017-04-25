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
			// JsonDocument u2 =
			// JsonDocument.create(String.valueOf(User.USER2.id),
			// User.USER2.toJsonObject());
			// JsonDocument u3 =
			// JsonDocument.create(String.valueOf(User.USER3.id),
			// User.USER3.toJsonObject());
			// JsonDocument u4 =
			// JsonDocument.create(String.valueOf(User.USER4.id),
			// User.USER4.toJsonObject());
			// JsonDocument u5 =
			// JsonDocument.create(String.valueOf(User.USER5.id),
			// User.USER5.toJsonObject());
			// JsonDocument u6 =
			// JsonDocument.create(String.valueOf(User.USER6.id),
			// User.USER6.toJsonObject());
			// JsonDocument u7 =
			// JsonDocument.create(String.valueOf(User.USER7.id),
			// User.USER7.toJsonObject());
			// JsonDocument u8 =
			// JsonDocument.create(String.valueOf(User.USER8.id),
			// User.USER8.toJsonObject());
			// JsonDocument u9 =
			// JsonDocument.create(String.valueOf(User.USER9.id),
			// User.USER9.toJsonObject());
			// JsonDocument u10 =
			// JsonDocument.create(String.valueOf(User.USER10.id),
			// User.USER10.toJsonObject());
			// JsonDocument u11 =
			// JsonDocument.create(String.valueOf(User.USER11.id),
			// User.USER11.toJsonObject());
			// JsonDocument u12 =
			// JsonDocument.create(String.valueOf(User.USER12.id),
			// User.USER12.toJsonObject());
			couchbaseBucket.upsert(u1);
			couchbaseBucket.upsert(uo);
			// couchbaseBucket.upsert(u2);
			// couchbaseBucket.upsert(u3);
			// couchbaseBucket.upsert(u4);
			// couchbaseBucket.upsert(u5);
			// couchbaseBucket.upsert(u6);
			// couchbaseBucket.upsert(u7);
			// couchbaseBucket.upsert(u8);
			// couchbaseBucket.upsert(u9);
			// couchbaseBucket.upsert(u10);
			// couchbaseBucket.upsert(u11);
			// couchbaseBucket.upsert(u12);
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
