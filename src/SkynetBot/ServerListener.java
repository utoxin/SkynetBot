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
			SkynetBot.db.updateUser(event.getUser(), event.getChannel());

			if (SkynetBot.db.isUserBanned(event.getUser(), event.getChannel())) {
				SkynetBot.bot.sendRawLine("CHANSERV KICK " + event.getChannel().getName() + " " + event.getUser().getNick() + " Exceeded the warning threshold.");
			}
		}
	}

	@Override
	public void onMessage( MessageEvent event ) {
		if (!event.getUser().getNick().equals(SkynetBot.bot.getNick())) {
			SkynetBot.db.updateUser(event.getUser(), event.getChannel());
		}

		updateLog(event.getChannel(), event.getUser(), event.getMessage(), event.getTimestamp());
		checkMessage(event.getChannel(), event.getUser(), event.getMessage());
	}

	@Override
	public void onAction( ActionEvent event ) {
		if (!event.getUser().getNick().equals(SkynetBot.bot.getNick())) {
			SkynetBot.db.updateUser(event.getUser(), event.getChannel());
		}

		updateLog(event.getChannel(), event.getUser(), event.getMessage(), event.getTimestamp());
		checkMessage(event.getChannel(), event.getUser(), event.getMessage());
	}

	protected void checkMessage( Channel channel, User user, String message ) {
		if (!user.getNick().equals("Timmy") && SkynetBot.db.badwords.get(channel.getName()) != null) {
			for (String word : SkynetBot.db.badwords.get(channel.getName())) {
				Pattern pattern = SkynetBot.db.badwordPatterns.get(word);

				if (pattern.matcher(message).find()) {
					handleViolation(channel, user, word);
					break;
				}
			}
		}
	}

	protected void handleViolation(Channel channel, User user, String word) {
		int warning_count = SkynetBot.db.getWarningCount(user, channel, true);

		if (warning_count >= 3) {
			SkynetBot.bot.sendRawLine("CHANSERV KICK " + channel.getName() + " " + user.getNick() + " Exceeded the warning threshold.");
			SkynetBot.db.banUser(user, channel);
		} else {
			SkynetBot.bot.sendMessage(channel, user.getNick() + ": WARNING! You have violated the policies of this channel! Please cease using the word '" + word + "' to avoid termination! You have " + warning_count + " warnings on file. 1 warning is removed every 24 hours you go without being warned again. 3 warnings results in a temporary ban from this channel.");
		}
	}
	
	protected void updateLog( Channel channel, User user, String message, double timestamp ) {
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
