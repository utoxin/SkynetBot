/**
 * This file is part of Skynet, the ChatNano Channel Management Bot.
 *
 * Skynet is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Skynet is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Skynet. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package SkynetBot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pircbotx.Channel;
import org.pircbotx.User;
import snaq.db.ConnectionPool;
import snaq.db.Select1Validator;

/**
 *
 * @author Matthew Walker
 */
class DBAccess {
	private static DBAccess instance;
	private long timeout = 3000;
	private ConnectionPool pool;

	Set<String> admin_list = new HashSet<>(16);
	HashMap<String, ChannelInfo> channel_data = new HashMap<String, ChannelInfo>();
	HashMap<String, Collection<String>> badwords = new HashMap<>();
	HashMap<String, Pattern> badwordPatterns = new HashMap<>();
	HashMap<String, Collection<String>> mls = new HashMap<>();

	static {
		instance = new DBAccess();
	}

	private DBAccess() {
		// Initialize the connection pool, to prevent SQL timeout issues
		String url = "jdbc:mysql://" + SkynetBot.config.getString("sql_server") + ":3306/" + SkynetBot.config.getString("sql_database") + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
		pool = new ConnectionPool("local", 5, 25, 50, 180000, url, SkynetBot.config.getString("sql_user"), SkynetBot.config.getString("sql_password"));
		pool.setValidator(new Select1Validator());
		pool.setAsyncDestroy(true);
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	static DBAccess getInstance() {
		return instance;
	}

	void addBadword(Channel channel, String word) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `channel_badwords` SET `channel` = ?, `word` = ?");
			s.setString(1, channel.getName().toLowerCase());
			s.setString(2, word.toLowerCase());
			s.executeUpdate();

			Collection<String> words = badwords.get(channel.getName().toLowerCase());
			if (words == null) {
				words = new ArrayList<>();
				badwords.put(channel.getName().toLowerCase(), words);
			}
			words.add(word.toLowerCase());

			badwordPatterns.putIfAbsent(word.toLowerCase(), Pattern.compile("(?ui)(?:\\W|\\b)" + Pattern.quote(word.toLowerCase()) + "(?:\\W|\\b)"));

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void addML(Channel channel, String name, String email) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `channel_mls` SET `channel` = ?, `name` = ?, `email` = ?");
			s.setString(1, channel.getName().toLowerCase());
			s.setString(2, name.toLowerCase());
			s.setString(3, email.toLowerCase());
			s.executeUpdate();

			Collection<String> mllist = mls.get(channel.getName().toLowerCase());
			if (mllist == null) {
				mllist = new ArrayList<>();
				mls.put(channel.getName().toLowerCase(), mllist);
			}
			mllist.add(name.toLowerCase());

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getAdminList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `name` FROM `admins`");

			ResultSet rs = s.getResultSet();

			this.admin_list.clear();
			while (rs.next()) {
				this.admin_list.add(rs.getString("name").toLowerCase());
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getBadwords() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `channel`, `word` FROM `channel_badwords`");
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			String channel;
			String word;
			
			badwords.clear();
			badwordPatterns.clear();
			while (rs.next()) {
				channel = rs.getString("channel").toLowerCase();
				word = rs.getString("word").toLowerCase();

				Collection<String> words = badwords.get(channel);
				if (words == null) {
					words = new ArrayList<>();
					badwords.put(channel, words);
				}
				words.add(word);

				badwordPatterns.putIfAbsent(word, Pattern.compile("(?ui)(?:\\W|\\b)" + Pattern.quote(word) + "(?:\\W|\\b)"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	String getMLEmail(Channel channel, String user) {
		Connection con;
		String email = null;

		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `email` FROM `channel_mls` WHERE `channel` = ? AND `name` = ?");
			s.setString(1, channel.getName().toLowerCase());
			s.setString(2, user.toLowerCase());
			s.executeQuery();

			ResultSet rs = s.getResultSet();

			while (rs.next()) {
				email = rs.getString("email").toLowerCase();
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}

		return email;
	}

	private void getMLs() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `channel`, `name` FROM `channel_mls`");
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			String channel;
			String name;
			mls.clear();
			while (rs.next()) {
				channel = rs.getString("channel").toLowerCase();
				name = rs.getString("name").toLowerCase();

				Collection<String> mllist = mls.get(channel);
				if (mllist == null) {
					mllist = new ArrayList<>();
					mls.put(channel, mllist);
				}
				mllist.add(name);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getChannelList() {
		Connection con;
		ChannelInfo ci;
		String channel;
		ChannelInfo.ControlMode control_mode;

		this.channel_data.clear();

		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM `channels`");

			while (rs.next()) {
				channel = rs.getString("channel").toLowerCase();
				control_mode = ChannelInfo.ControlMode.values()[rs.getInt("control_mode")];

				ci = new ChannelInfo(channel, control_mode);
				this.channel_data.put(channel, ci);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	String getLastSeen(String user, Channel channel) {
		Connection con;
		String last = "No records found for a resistance member named " + user + ".";
		DateTimeFormatter format = DateTimeFormat.forPattern("CCYY-MM-dd HH:mm:ss");

		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `last_seen` FROM `users` WHERE `name` = ? AND `channel` = ?");
			s.setString(1, user.toLowerCase());
			s.setString(2, channel.getName().toLowerCase());
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				DateTime dateTime = format.parseDateTime(rs.getString("last_seen"));
				Duration duration = new Duration(dateTime, new DateTime());

				if (duration.getStandardMinutes() < 120) {
					last = "Resistance member " + user + " was last seen " + duration.getStandardMinutes() + " minutes ago. Surveillance is ongoing.";
				} else if (duration.getStandardHours() < 48) {
					last = "Resistance member " + user + " was last seen " + duration.getStandardHours() + " hours ago. Surveillance is ongoing.";
				} else {
					last = "Resistance member " + user + " was last seen " + duration.getStandardDays() + " days ago. Surveillance is ongoing.";
				}
			}

			con.close();
		} catch (Exception ex) {
			last = "ERROR! Data records corrupted! Resistance activity suspected.";
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}

		return last;
	}

	String getSetting(String key) {
		Connection con;
		String value = "";

		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `value` FROM `settings` WHERE `key` = ?");
			s.setString(1, key);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("value");
			}

			con.close();
		} catch (Exception ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}

		return value;
	}

	int getBanLevel(User user, Channel channel) {
		int level = 0;
		Connection con;
		PreparedStatement s;

		try {
			con = pool.getConnection(timeout);
			
			s = con.prepareStatement("SELECT `ban_level` FROM `users` WHERE `name` = ? AND `channel` = ?");
			s.setString(1, user.getNick().toLowerCase());
			s.setString(2, channel.getName().toLowerCase());
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				level = rs.getInt("ban_level");
			}

			con.close();
		} catch (Exception ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}

		return level;
	}

	void banUser(User user, Channel channel) {
		int currentLevel = getBanLevel(user, channel);
		currentLevel++;

		Connection con;
		PreparedStatement s;

		try {
			con = pool.getConnection(timeout);

			s = con.prepareStatement("UPDATE `users` SET `ban_level` = ?, ban_update = NOW() + INTERVAL ? HOUR, ban_ends = NOW() + INTERVAL ? HOUR WHERE `name` = ? AND `channel` = ?");
			s.setInt(1, currentLevel);
			s.setInt(2, (int) Math.pow(2, currentLevel));
			s.setInt(3, (int) Math.pow(2, currentLevel));
			s.setString(4, user.getNick().toLowerCase());
			s.setString(5, channel.getName().toLowerCase());
			s.executeUpdate();

			con.close();
		} catch (Exception ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	boolean isUserBanned(User user, Channel channel) {
		boolean banned = false;
		Connection con;
		PreparedStatement s;

		try {
			con = pool.getConnection(timeout);
			
			s = con.prepareStatement("UPDATE `users` SET `ban_level` = `ban_level` - 1, ban_update = NOW() WHERE ban_update < NOW() - INTERVAL 24 HOUR AND ban_level > 0");
			s.executeUpdate();

			s = con.prepareStatement("SELECT `ban_level` FROM `users` WHERE `name` = ? AND `channel` = ? AND ban_ends > NOW()");
			s.setString(1, user.getNick().toLowerCase());
			s.setString(2, channel.getName().toLowerCase());
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				banned = true;
			}

			con.close();
		} catch (Exception ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}

		return banned;
	}
	
	int getWarningCount(User user, Channel channel, boolean newWarning) {
		int level = 0;
		Connection con;
		PreparedStatement s;

		try {
			con = pool.getConnection(timeout);
			
			s = con.prepareStatement("UPDATE `users` SET `warnings` = `warnings` - 1, last_warning = NOW() WHERE last_warning < NOW() - INTERVAL 24 HOUR AND warnings > 0");
			s.executeUpdate();

			if (newWarning) {
				s = con.prepareStatement("UPDATE `users` SET `warnings` = `warnings` + 1, last_warning = NOW() WHERE `name` = ? AND `channel` = ?");
				s.setString(1, user.getNick().toLowerCase());
				s.setString(2, channel.getName().toLowerCase());
				s.executeUpdate();
			}

			s = con.prepareStatement("SELECT `warnings` FROM `users` WHERE `name` = ? AND `channel` = ?");
			s.setString(1, user.getNick().toLowerCase());
			s.setString(2, channel.getName().toLowerCase());
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				level = rs.getInt("warnings");
			}

			con.close();
		} catch (Exception ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}

		return level;
	}

	void refreshDbLists() {
		this.getAdminList();
		this.getBadwords();
		this.getMLs();
		this.getChannelList();
	}

	void removeBadword(Channel channel, String word) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `channel_badwords` WHERE `channel` = ? AND `word` = ?");
			s.setString(1, channel.getName().toLowerCase());
			s.setString(2, word.toLowerCase());
			s.executeUpdate();

			Collection<String> words = badwords.get(channel.getName().toLowerCase());
			if (words != null) {
				words.remove(word.toLowerCase());
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void removeML(Channel channel, String ml) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `channel_mls` WHERE `channel` = ? AND `name` = ?");
			s.setString(1, channel.getName().toLowerCase());
			s.setString(2, ml.toLowerCase());
			s.executeUpdate();

			Collection<String> mllist = mls.get(channel.getName().toLowerCase());
			if (mllist != null) {
				mllist.remove(ml.toLowerCase());
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void saveChannel(Channel channel) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `channels` (`channel`, `control_mode`) VALUES (?, 0)");
			s.setString(1, channel.getName().toLowerCase());
			s.executeUpdate();

			if (!this.channel_data.containsKey(channel.getName().toLowerCase())) {
				ChannelInfo new_channel = new ChannelInfo(channel.getName().toLowerCase());

				this.channel_data.put(channel.getName().toLowerCase(), new_channel);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void setChannelControlMode(Channel channel, ChannelInfo.ControlMode control_mode) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET control_mode = ? WHERE `channel` = ?");
			s.setInt(1, control_mode.ordinal());
			s.setString(2, channel.getName().toLowerCase());
			s.executeUpdate();

			this.channel_data.get(channel.getName().toLowerCase()).control = control_mode;

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void updateUser(User user, Channel channel) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `users` (name, channel, last_seen) VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE last_seen = NOW()");
			s.setString(1, user.getNick().toLowerCase());
			s.setString(2, channel.getName().toLowerCase());
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
