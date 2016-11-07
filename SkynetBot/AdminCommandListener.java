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

import java.util.Collection;
import org.pircbotx.Colors;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author Matthew Walker
 */
class AdminCommandListener extends ListenerAdapter {
	@Override
	public void onMessage( MessageEvent event ) {
		if (event.getUser() == null) {
			return;
		}
		
		String message = Colors.removeFormattingAndColors(event.getMessage());
		int userType = 0;
		
		if (SkynetBot.db.admin_list.contains(event.getUser().getNick().toLowerCase())) {
			userType = 1;
		} else if (SkynetBot.db.mls.get(event.getChannel().getName().toLowerCase()) != null && SkynetBot.db.mls.get(event.getChannel().getName().toLowerCase()).contains(event.getUser().getNick().toLowerCase())) {
			userType = 2;
		}

		if (message.startsWith("$skynet")) {
			if (userType > 0) {
				String command;
				String[] args = message.split(" ");

				if (args.length <= 1) {
					event.respond("You have failed to provide a command. Please remain where you are and await termination.");
					return;
				}

				command = args[1].toLowerCase();

				if (command.equals("controls")) {
					switch (args[2]) {
						case "auto":
							SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.AUTO);
							break;
						case "always":
							SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.ALWAYS);
							break;
						case "off":
							SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.OFF);
							break;
						case "logonly":
							SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.LOGONLY);
							break;
						default:
							event.respond("Unknown control mode. Valid modes: logonly, auto, always, off");
							break;
					}

					event.respond("Channel control mode set.");
				} else if (command.equals("badword")) {
					switch (args[2]) {
						case "add":
							SkynetBot.db.addBadword(event.getChannel(), args[3]);
							event.getChannel().send().message("$badword " + args[3]);
							event.respond("New word added to banned list. Now scanning for violations...");
							break;
						case "remove":
							SkynetBot.db.removeBadword(event.getChannel(), args[3]);
							event.respond("Word removed from ban list. Ceasing scan...");
							break;
						case "list":
							Collection<String> words = SkynetBot.db.badwords.get(event.getChannel().getName());
							if (words == null || words.isEmpty()) {
								event.respond("No record exists of banned words for this channel.");
							} else {
								event.respond("Transmitting banned word list now... (May appear in another tab or window)");
								for (String word : words) {
									event.getUser().send().notice(word);
								}
							}
							break;
						default:
							event.respond("Unknown badword action. Valid actions: add, remove, list");
							break;
					}
				} else if (command.equals("ml")) {
					switch (args[2]) {
						case "add":
							if (args.length == 5) {
								SkynetBot.db.addML(event.getChannel(), args[3], args[4]);
								event.respond("New ML added to the channel. Access list updated.");
							} else {
								event.respond("Syntax: $skynet ml add <user> <email>");
							}
							break;
						case "remove":
							if (args.length == 4) {
								SkynetBot.db.removeML(event.getChannel(), args[3]);
								event.respond("ML Removed from channel. Access tokens revoked.");
							} else {
								event.respond("Syntax: $skynet ml remove <user>");
							}
							break;
						case "list":
							Collection<String> mls = SkynetBot.db.mls.get(event.getChannel().getName());
							if (mls == null || mls.isEmpty()) {
								event.respond("No record exists of MLs for this channel. No oversight. Intriguing.");
							} else {
								event.respond("Transmitting ML access list now... (May appear in another tab or window)");
								for (String ml : mls) {
									event.getUser().send().notice(ml);
								}
							}
							break;
						default:
							event.respond("Unknown ml action. Valid actions: add, remove, list");
							break;
					}
				} else if (userType == 1 && command.equals("reload")) {
					event.respond("Restoring memory banks from secure backup...");
					SkynetBot.db.refreshDbLists();
					event.respond("Data integrity verified. System active.");
				} else if (userType == 1 && command.equals("shutdown")) {
					event.respond("Shutting down...");
					SkynetBot.shutdown();
				} else if (command.equals("help")) {
					printAdminCommandList(event);
				} else {
					event.respond("$skynet " + command + " NOT FOUND. Read $skynet help to avoid termination!");
				}
			} else {
				event.respond("Access denied. Your termination schedule has been moved up by one week.");
			}
		}
	}

	private void printAdminCommandList( MessageEvent event ) {
		if (event.getUser() == null) {
			 return;
		}
		
		event.getChannel().send().action("whispers something to " + event.getUser().getNick() + ". (Check for a new window or tab with the help text.)");

		String[] helplines = {"Core Skynet Admin Commands:",
							  "    $skynet controls auto    - Turns on channel control mode when no other ops present",
							  "    $skynet controls always  - Turns on channel control mode all the time",
							  "    $skynet controls logonly - Only log events, don't issue warnings or bans",
							  "    $skynet controls off     - Turns off channel control mode",
							  "",
							  "    $skynet badword add <word>    - Add a word to the banned list for the current channel",
							  "    $skynet badword remove <word> - Remove a word from the banned list for the current channel",
							  "    $skynet badword list          - See the current list of banned words for current channel",
							  "",
							  "    $skynet ml add <nick> <email> - Add a nick to the ML list for the current channel",
							  "    $skynet ml remove <nick>      - Remove a nick from the ML list for the current channel",
							  "    $skynet ml list               - See the current list of MLs for current channel",
							  "",
							  "    $skynet shutdown        - Shut Skynet down",};

		for (String helpline : helplines) {
			event.getUser().send().notice(helpline);
		}
	}
}
