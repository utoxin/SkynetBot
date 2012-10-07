/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package skynetbot;

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