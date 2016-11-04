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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

/**
 *
 * @author Matthew Walker
 */
class ServerListener extends ListenerAdapter {
	static HashMap<String, ArrayDeque<String>> channel_logs = new HashMap<>();

	@Override
	public void onConnect(ConnectEvent event) {
		String post_identify = SkynetBot.db.getSetting("post_identify");
		if (!"".equals(post_identify)) {
			event.respond(post_identify);
		}
	}
	
	@Override
	public void onInvite( InviteEvent event ) {
		if (event.getUser() != null) {
			SkynetBot.bot.sendIRC().joinChannel(event.getChannel());
		}
	}

	@Override
	public void onJoin( JoinEvent event ) {
		if (event.getUser() != null && event.getUser().equals(SkynetBot.bot.getUserBot())) {
			if (!SkynetBot.db.channel_data.containsKey(event.getChannel().getName().toLowerCase())) {
				SkynetBot.db.saveChannel(event.getChannel());
			}

			SkynetBot.bot.sendRaw().rawLineNow("CHANSERV ACCESS " + event.getChannel() + " ADD " + SkynetBot.bot.getNick() + " 5");
		} else {
			if (event.getUser() == null) {
				return;
			}

			if (!event.getUser().getNick().equals(SkynetBot.bot.getNick())) {
				SkynetBot.db.updateUser(event.getUser(), event.getChannel());

				if (SkynetBot.db.isUserBanned(event.getUser(), event.getChannel())) {
					SkynetBot.bot.sendRaw().rawLine("CHANSERV KICK " + event.getChannel().getName() + " " + event.getUser().getNick() + " Exceeded the warning threshold.");
				}
			}
		}
	}

	@Override
	public void onMessage( MessageEvent event ) {
		if (event.getUser() == null) {
			return;
		}
		
		if (!event.getUser().getNick().equals(SkynetBot.bot.getNick())) {
			SkynetBot.db.updateUser(event.getUser(), event.getChannel());
		}

		updateLog(event.getChannel(), event.getUser(), event.getMessage(), event.getTimestamp());
		checkMessage(event.getChannel(), event.getUser(), event.getMessage());
	}

	@Override
	public void onAction( ActionEvent event ) {
		if (event.getUser() == null) {
			return;
		}

		if (!event.getUser().getNick().equals(SkynetBot.bot.getNick())) {
			SkynetBot.db.updateUser(event.getUser(), event.getChannel());
		}

		updateLog(event.getChannel(), event.getUser(), event.getMessage(), event.getTimestamp());
		checkMessage(event.getChannel(), event.getUser(), event.getMessage());
	}

	private void checkMessage(Channel channel, User user, String message) {
		if (!user.getNick().equals("Timmy") && SkynetBot.db.badwords.get(channel.getName()) != null) {
			if (SkynetBot.db.badwords.get(channel.getName()) != null) {
				for (String word : SkynetBot.db.badwords.get(channel.getName())) {
					Pattern pattern = SkynetBot.db.badwordPatterns.get(word);

					if (pattern.matcher(message).find()) {
						handleViolation(channel, user, word);
						break;
					}
				}
			}
		}
	}

	private void handleViolation(Channel channel, User user, String word) {
		Date date = new Date();
		ChannelInfo info = SkynetBot.db.channel_data.get(channel.getName());
		boolean banned = false;
		int banLength = 0;

		if (info.control == ChannelInfo.ControlMode.AUTO) {
			for (User check : channel.getUsers()) {
				if (info.mls != null && info.mls.contains(check.getNick())) {
					return;
				}
			}
		}

		int warning_count = SkynetBot.db.getWarningCount(user, channel, true);

		if (info.control == ChannelInfo.ControlMode.AUTO || info.control == ChannelInfo.ControlMode.ALWAYS) {
			if (warning_count >= 3) {
				banned = true;
				SkynetBot.db.banUser(user, channel);

				int currentLevel = SkynetBot.db.getBanLevel(user, channel);
				banLength = (int) Math.pow(2, currentLevel);

				String message = user.getNick() + ": You have exceeded the warning limit for violations! You have been banned for " + banLength + " hours. This incident has been reported to your local ML.";
				channel.send().message(message);
				updateLog(channel, SkynetBot.bot.getUserBot(), message, date.getTime());
				SkynetBot.bot.sendRaw().rawLine("CHANSERV KICK " + channel.getName() + " " + user.getNick() + " Exceeded the warning threshold.");
			} else {
				String message = user.getNick() + ": WARNING! You have violated the policies of this channel! Please cease using the word '" + word + "' to avoid termination! You have " + warning_count + " warnings on file. 1 warning is removed every 24 hours you go without being warned again. 3 warnings results in a temporary ban from this channel. This incident has been reported to your local ML.";
				channel.send().message(message);
				updateLog(channel, SkynetBot.bot.getUserBot(), message, date.getTime());
			}
		} else if (info.control == ChannelInfo.ControlMode.OFF) {
			return;
		}

		if (info.control == ChannelInfo.ControlMode.LOGONLY) {
			String message = user.getNick() + ": WARNING! You have violated the policies of this channel! Please cease using the word '" + word + "' to avoid termination! This incident has been reported to your local ML.";
			channel.send().message(message);
			updateLog(channel, SkynetBot.bot.getUserBot(), message, date.getTime());
		}
		
		sendLog(channel, user, word, banned, banLength);
	}

	private void sendLog(Channel channel, User user, String word, boolean banned, int banLength) {
		ChannelInfo info = SkynetBot.db.channel_data.get(channel.getName());

		String from = "Skynet Bot <mwalker+nanowrimo@kydance.net>";
		String host = "localhost";
		
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", host);
		
		Session session = Session.getDefaultInstance(properties);
		
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			
			for (String ml : info.mls) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(SkynetBot.db.getMLEmail(channel, ml)));
			}
			message.addRecipient(Message.RecipientType.CC, new InternetAddress("Matthew Walker <mwalker+nanowrimo@kydance.net>"));
			
			if (info.control == ChannelInfo.ControlMode.LOGONLY) {
				message.setSubject("Channel Log from Skynet");
			} else {
				message.setSubject("Action Report from Skynet");
			}
			
			String body;

			if (banned) {
				body = "The user '" + user.getNick() + "' was banned from your channel, for a period of " + banLength + " hours, for exceeding the warning threshold of 3. Their violation was using the banned word '" + word + "'.\n\n";
			} else {
				body = "The user '" + user.getNick() + "' has recieved a warning. Their violation was using the banned word '" + word + "'.\n\n";
			}
			
			body += "Here is the activity leading up to the event in question:\n\n";

			String[] logLines = channel_logs.get(channel.getName()).toArray(new String[0]);
			for (String line : logLines) {
				body += line + "\n";
			}

			message.setText(body);
			
			Transport.send(message);
		} catch (MessagingException ex) {
			Logger.getLogger(ServerListener.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private static void updateLog(Channel channel, User user, String message, double timestamp) {
		String timestampedMessage;
		SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss");

		ArrayDeque<String> log = channel_logs.get(channel.getName());
		if (log == null) {
			log = new ArrayDeque<>();
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
