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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.exception.NickAlreadyInUseException;

/**
 *
 * @author Matthew Walker
 */
public class SkynetBot {
	public static PircBotX bot;
	public static AppConfig config = AppConfig.getInstance();
	public static DBAccess db = DBAccess.getInstance();
	public static SkynetBot instance;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		instance = new SkynetBot();
	}

	public SkynetBot() {
		bot = new PircBotX();

		bot.getListenerManager().addListener(new ServerListener());
		bot.getListenerManager().addListener(new AdminCommandListener());
		bot.getListenerManager().addListener(new UserCommandListener());

		bot.setEncoding(Charset.forName("UTF-8"));
		bot.setLogin("Cyberdyne");
		bot.setMessageDelay(Long.parseLong(db.getSetting("max_rate")));
		bot.setName(db.getSetting("nickname"));
		bot.setVerbose(true);

		try {
			bot.connect(db.getSetting("server"));
		} catch (IOException ex) {
			Logger.getLogger(SkynetBot.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NickAlreadyInUseException ex) {
			Logger.getLogger(SkynetBot.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IrcException ex) {
			Logger.getLogger(SkynetBot.class.getName()).log(Level.SEVERE, null, ex);
		}

		bot.identify(db.getSetting("password"));

		String post_identify = db.getSetting("post_identify");
		if (!"".equals(post_identify)) {
			bot.sendRawLineNow(post_identify);
		}

		db.refreshDbLists();

		// Join our channels
		for (Map.Entry<String, ChannelInfo> entry : db.channel_data.entrySet()) {
			bot.joinChannel(entry.getValue().channel.getName());
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