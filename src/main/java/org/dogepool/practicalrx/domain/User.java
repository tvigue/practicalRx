package org.dogepool.practicalrx.domain;

import com.couchbase.client.java.document.json.JsonObject;

public class User {

	public static final User USER = new User(0L, "user0", "Test User", "Story of my life.\nEnd of Story.", "12434");
	public static final User OTHERUSER = new User(1L, "richUser", "Richie Rich", "I'm rich I have dogecoin", "45678");
	public static final User USER2 = new User(2L, "user2", "Lorem", "Hello World!", "0");
	public static final User USER3 = new User(3L, "user3", "Lorem ipsum", "42", "1");
	public static final User USER4 = new User(4L, "user4", "Lorem ipsum dolor", "I'm rich I have dogecoin", "3");
	public static final User USER5 = new User(5L, "user5", "Lorem ipsum dolor sit", "OWK", "4");
	public static final User USER6 = new User(6L, "user6", "Lorem ipsum dolor sit amet", "1337", "5");
	public static final User USER7 = new User(7L, "user7", "Lorem ipsum dolor sit amet, consectetur", "Test", "7");
	public static final User USER8 = new User(8L, "user8", "58fe01dd-075c-4eb5-a", "42", "1");
	public static final User USER9 = new User(9L, "user9", "36862703387513870263217969548349", "I'm rich I have dogecoin", "3");
	public static final User USER10 = new User(10L, "user10", "y/A]sGrKBjOdVih_ABZKEZJzyqx{|<2`Z6h5l.bheCvf93", "OWK", "4");
	public static final User USER11 = new User(11L, "user11", "abd>SnTOvGVZtn", "1337", "5");
	public static final User USER12 = new User(12L, "user12", "+ K&ZS$L/4]8cTqBE",
			"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum non tortor fringilla, iaculis nibh "
			+ "quis, iaculis augue. Phasellus vulputate ullamcorper mollis. Pellentesque rhoncus sodales hendrerit. "
			+ "In hac habitasse platea dictumst. Suspendisse laoreet volutpat maximus. Nulla vel posuere mauris. "
			+ "Integer commodo, est ut blandit fringilla, massa leo lobortis urna, eget ultricies justo tellus ac "
			+ "velit. Donec ipsum ipsum, sodales sed dolor in, mollis rutrum nisi. Ut ultricies felis nulla, sit amet "
			+ "eleifend lectus condimentum id.", "7");

	public final long id;
	public final String nickname;
	public final String displayName;
	public final String bio;
	public final String avatarId;
	public final String type = "user";

	public User(long id, String nickname, String displayName, String bio, String avatarId) {
		this.id = id;
		this.nickname = nickname;
		this.displayName = displayName;
		this.bio = bio;
		this.avatarId = avatarId;
	}

	public JsonObject toJsonObject() {
		JsonObject jso = JsonObject.create();
		jso.put("id", id);
		jso.put("nickname", nickname);
		jso.put("displayName", displayName);
		jso.put("bio", bio);
		jso.put("avatarId", avatarId);
		jso.put("type", "user");
		return jso;
	}

	public static User fromJsonObject(JsonObject jso) {
		return new User(jso.getInt("id"), jso.getString("nickname"), jso.getString("displayName"), jso.getString("bio"),
				jso.getString("avatarId"));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		User user = (User) o;

		if (id != user.id) {
			return false;
		}
		if (!nickname.equals(user.nickname)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + nickname.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return displayName + " (" + nickname + ")";
	}

}
