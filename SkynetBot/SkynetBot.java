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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

/**
 *
 * @author Matthew Walker
 */
public class SkynetBot {
	static AppConfig config = AppConfig.getInstance();

	public static PircBotX bot;
	public static DBAccess db = DBAccess.getInstance();

	private static SkynetBot instance;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		instance = new SkynetBot();
	}

	public SkynetBot() {
		Configuration.Builder configBuilder = new Configuration.Builder()
				.setName(db.getSetting("nickname"))
				.setLogin("Cyberdyne")
				.setNickservPassword(db.getSetting("password"))
				.addServer(db.getSetting("server"))
				.setServerPassword(db.getSetting("server_password"))
				.setEncoding(Charset.forName("UTF-8"))
				.setMessageDelay(Long.parseLong(db.getSetting("max_rate")))
				.setAutoNickChange(true)
				.addListener(new ServerListener())
				.addListener(new AdminCommandListener())
				.addListener(new UserCommandListener())
				.setNickservDelayJoin(true);

		db.refreshDbLists();
		db.channel_data.entrySet().forEach((entry) -> configBuilder.addAutoJoinChannel(entry.getValue().channel));

		bot = new PircBotX(configBuilder.buildConfiguration());

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (SkynetBot.bot.isConnected()) {
					try {
						SkynetBot.shutdown();
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						Logger.getLogger(SkynetBot.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		});

		try {
			bot.startBot();
		} catch (IOException | IrcException ex) {
			Logger.getLogger(SkynetBot.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	static void shutdown() {
		if (SkynetBot.bot.isConnected()) {
			SkynetBot.bot.stopBotReconnect();
			SkynetBot.bot.sendIRC().quitServer("I'll be back!");
		}
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static SkynetBot getInstance() {
		return instance;
	}
}