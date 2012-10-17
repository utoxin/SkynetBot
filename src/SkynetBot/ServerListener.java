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

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author Matthew Walker
 */
public class ServerListener extends ListenerAdapter {
	protected HashMap<String, ArrayDeque<String>> channel_logs = new HashMap<String, ArrayDeque<String>>();

	@Override
	public void onInvite( InviteEvent event ) {
		if (!SkynetBot.db.channel_data.containsKey(event.getChannel())) {
			SkynetBot.bot.joinChannel(event.getChannel());
			SkynetBot.db.saveChannel(SkynetBot.bot.getChannel(event.getChannel()));
			SkynetBot.bot.sendRawLineNow("CHANSERV ACCESS " + event.getChannel() + " ADD " + SkynetBot.bot.getNick() + " 5");
		}
	}

	@Override
	public void onJoin( JoinEvent event ) {
		if (!event.getUser().getNick().equals(SkynetBot.bot.getNick())) {
			if (!Pattern.matches("(?i)mib_......", event.getUser().getNick()) && !Pattern.matches("(?i)guest.*", event.getUser().getNick())) {
				SkynetBot.db.updateUser(event.getUser(), event.getChannel());
			}
		}
	}

	@Override
	public void onMessage( MessageEvent event ) {
		if (!event.getUser().getNick().equals(SkynetBot.bot.getNick())) {
			if (!Pattern.matches("(?i)mib_......", event.getUser().getNick()) && !Pattern.matches("(?i)guest.*", event.getUser().getNick())) {
				SkynetBot.db.updateUser(event.getUser(), event.getChannel());
			}
		}

		updateLog(event.getChannel(), event.getMessage(), event.getTimestamp(), event.getUser());
	}

	@Override
	public void onAction( ActionEvent event ) {
		if (!event.getUser().getNick().equals(SkynetBot.bot.getNick())) {
			if (!Pattern.matches("(?i)mib_......", event.getUser().getNick()) && !Pattern.matches("(?i)guest.*", event.getUser().getNick())) {
				SkynetBot.db.updateUser(event.getUser(), event.getChannel());
			}
		}

		updateLog(event.getChannel(), event.getMessage(), event.getTimestamp(), event.getUser());
	}

	protected void updateLog( Channel channel, String message, double timestamp, User user ) {
		String timestampedMessage;
		SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss");

		ArrayDeque<String> log = channel_logs.get(channel.getName());
		if (log == null) {
			log = new ArrayDeque<String>();
			channel_logs.put(channel.getName(), log);
		}

		timestampedMessage = "[" + dateFormatter.format(timestamp) + "] ";
		timestampedMessage += "<" + user.getNick() + "> ";
		timestampedMessage += message;
		log.addLast(timestampedMessage);

		while (log.size() > 25) {
			log.removeFirst();
		}
	}
}
