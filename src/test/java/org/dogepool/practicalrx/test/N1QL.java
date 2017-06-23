package org.dogepool.practicalrx.test;

import static com.couchbase.client.java.query.N1qlQuery.simple;
import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static java.lang.System.err;
import static java.lang.System.out;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple5;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.N1qlMetrics;
import com.couchbase.client.java.query.N1qlQueryResult;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Observable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class N1QL {

	private Bucket beerSample;
	private CouchbaseCluster cluster;

	@Test
	public void selectOnBeerSync() {
		// Simple select on sample beer bucket
		N1qlQueryResult beers = beerSample.query(simple(select("*").from(i("beer-sample")).limit(10)));

		assertEquals(10, beers.info().resultCount());

		out.println(beers.errors().toString());
		out.println(beers.info().elapsedTime());
		out.println(beers.info().resultCount());
	}

	@Test
	public void selectOnBeerAsync() {
		// Simple select on sample beer async bucket
		Observable<AsyncN1qlQueryResult> beers = RxJavaInterop
				.toV2Observable(beerSample.async().query(simple(select("*").from(i("beer-sample")).limit(10))));
		// Handling async result
		beers.subscribe(result -> {
			result.errors().subscribe(errors -> out.println(errors.toString()));
			result.info().subscribe(info -> {
				assertEquals(10, info.resultCount());
				out.println(info.elapsedTime());
				out.println(info.resultCount());
			});
		});

		beers.blockingSingle(); // We block to force junit
								// into waiting for the
								// result
	}

	@RequiredArgsConstructor(staticName = "of")
	@Getter
	private static class N1qlResultHolder {

		private final List<JsonObject> results;
		private final List<JsonObject> errors;
		private final boolean parsed;
		private final boolean success;
		private final JsonObject metrics;

	}

	@Test
	public void selectOnBeerAsyncAdvanced() {
		// Simple select on sample beer async bucket
		rx.Observable<AsyncN1qlQueryResult> beers = beerSample.async()
				.query(simple(select("*").from(i("beer-sample")).limit(10)));

		Observable<Tuple5> datas = RxJavaInterop.toV2Observable(rx.Observable.zip(
				beers.map(AsyncN1qlQueryResult::parseSuccess), beers.flatMap(AsyncN1qlQueryResult::finalSuccess),
				beers.flatMap(AsyncN1qlQueryResult::rows).map(AsyncN1qlQueryRow::value).toList(),
				beers.flatMap(results -> results.errors()).toList(),
				beers.flatMap(AsyncN1qlQueryResult::info).map(N1qlMetrics::asJsonObject),
				(parsed, status, values, errors, metrics) -> Tuple.create(values, errors, parsed, status, metrics)));

		datas.subscribe(queryResult -> {
			err.println("RESULTS : " + queryResult.value3());
			err.println("ERRORS : " + queryResult.value4());
			err.println("PARSE STATUS : " + queryResult.value1());
			err.println("FINAL STATUS : " + queryResult.value2());
			err.println("METRICS : " + queryResult.value5());
		});

		datas.blockingSingle(); // We block to force junit
								// into waiting for the
								// result
	}

	@Before
	public void before() {
		CouchbaseEnvironment env = DefaultCouchbaseEnvironment.create();
		cluster = CouchbaseCluster.create(env, "10.142.162.101");
		beerSample = cluster.openBucket("beer-sample");
		// Needed to use N1QL
		beerSample.query(simple(Index.createPrimaryIndex().on("beer-sample")));
	}

	@After
	public void after() {
		cluster.disconnect();
	}

}
