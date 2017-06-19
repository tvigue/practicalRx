package org.dogepool.practicalrx.services;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

import org.dogepool.practicalrx.domain.User;
import org.dogepool.practicalrx.error.DogePoolException;
import org.dogepool.practicalrx.error.Error;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.Statement;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.BackpressureStrategy;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;

/**
 * Service to get user information.
 */
@Service
public class UserService {

	@Autowired(required = false)
	private Bucket couchbaseBucket;

	@Value("${store.enableFindAll:false}")
	private boolean useCouchbaseForFindAll;

	public Flux<User> getUser(long id) {
		return findAll().filter(u -> u.id == id).take(1);
	}

	public Flux<User> getUserByLogin(String login) {
		return findAll().filter(u -> login.equals(u.nickname)).take(1);
	}

	public Flux<User> findAll() {
		if (useCouchbaseForFindAll && couchbaseBucket != null) {
			try {
				Statement statement = Select.select("avatarId", "bio", "displayName", "id", "nickname")
						.from(i(couchbaseBucket.name()))
						.where(x("type")
								.eq(s("user")))/* .groupBy(x("displayName")) */;
				rx.Observable<AsyncN1qlQueryResult> queryResult = couchbaseBucket.async().query(statement);
				return RxJava2Adapter.observableToFlux(
						RxJavaInterop.toV2Observable(queryResult.flatMap(AsyncN1qlQueryResult::rows)
								.map(AsyncN1qlQueryRow::value)
								.map(qr -> User.fromJsonObject(qr))),
						BackpressureStrategy.ERROR);
			} catch (Exception e) {
				return Flux.error(new DogePoolException("Error while getting list of users from database",
						Error.DATABASE, HttpStatus.INTERNAL_SERVER_ERROR, e));
			}
		} else {
			return Flux.just(User.USER, User.OTHERUSER);
		}
	}

}
