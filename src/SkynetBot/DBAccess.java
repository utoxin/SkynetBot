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

import com.mysql.jdbc.Driver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class DBAccess {
	private static DBAccess instance;
	private long timeout = 3000;
	protected ConnectionPool pool;
	protected Set<String> admin_list = new HashSet<String>(16);
	protected HashMap<String, ChannelInfo> channel_data = new HashMap<String, ChannelInfo>(62);

	static {
		instance = new DBAccess();
	}

	private DBAccess() {
		Class c;
		Driver driver;

		/**
		 * Make sure the JDBC driver is initialized. Used by the connection pool.
		 */
		try {
			c = Class.forName("com.mysql.jdbc.Driver");
			driver = (Driver) c.newInstance();
			DriverManager.registerDriver(driver);
		} catch (Exception ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Initialize the connection pool, to prevent SQL timeout issues
		String url = "jdbc:mysql://" + SkynetBot.config.getString("sql_server") + ":3306/" + SkynetBot.config.getString("sql_database") + "?useUnicode=true&characterEncoding=UTF-8";
		pool = new ConnectionPool("local", 5, 25, 50, 180000, url, SkynetBot.config.getString("sql_user"), SkynetBot.config.getString("sql_password"));
		pool.setValidator(new Select1Validator());
		pool.setAsyncDestroy(true);
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static DBAccess getInstance() {
		return instance;
	}

	public void getAdminList() {
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

	public void getChannelList() {
		Connection con;
		ChannelInfo ci;
		Channel channel;
		ChannelInfo.ControlMode control_mode;

		this.channel_data.clear();

		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM `channels`");

			while (rs.next()) {
				channel = SkynetBot.bot.getChannel(rs.getString("channel"));
				control_mode = ChannelInfo.ControlMode.values()[rs.getInt("control_mode")];
				
				ci = new ChannelInfo(channel, control_mode);

				this.channel_data.put(channel.getName(), ci);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public String getLastSeen( String user, Channel channel ) {
		Connection con;
		String last = "No records found for a resistance member named " + user + ".";
		DateTimeFormatter format = DateTimeFormat.forPattern("CCYY-MM-dd HH:mm:ss.S");

		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `last_seen` FROM `users` WHERE `name` = ? AND `channel` = ?");
			s.setString(1, user);
			s.setString(2, channel.getName());
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
	
	public String getSetting( String key ) {
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

	public void refreshDbLists() {
		this.getAdminList();
		this.getChannelList();
	}

	public void saveChannel( Channel channel ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `channels` (`channel`, `control_mode`) VALUES (?, 0)");
			s.setString(1, channel.getName());
			s.executeUpdate();

			if (!this.channel_data.containsKey(channel.getName())) {
				ChannelInfo new_channel = new ChannelInfo(channel);

				this.channel_data.put(channel.getName(), new_channel);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setChannelControlMode( Channel channel, ChannelInfo.ControlMode control_mode ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET control_mode = ? WHERE `channel` = ?");
			s.setInt(1, control_mode.ordinal());
			s.setString(2, channel.getName());
			s.executeUpdate();

			this.channel_data.get(channel.getName()).control = control_mode;

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void updateUser ( User user, Channel channel ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);
			
			PreparedStatement s = con.prepareStatement("INSERT INTO `users` (name, channel, last_seen) VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE last_seen = NOW()");
			s.setString(1, user.getNick());
			s.setString(2, channel.getName());
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
