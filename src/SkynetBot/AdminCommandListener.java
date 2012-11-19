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
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.Colors;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author Matthew Walker
 */
public class AdminCommandListener extends ListenerAdapter {
	@Override
	public void onMessage( MessageEvent event ) {
		String message = Colors.removeFormattingAndColors(event.getMessage());

		if (message.startsWith("$skynet")) {
			if (SkynetBot.db.admin_list.contains(event.getUser().getNick().toLowerCase())) {
				String command;
				String[] args = message.split(" ");

				if (args.length <= 1) {
					event.respond("You have failed to provide a command. Please remain where you are and await termination.");
					return;
				}

				command = args[1].toLowerCase();

				if (command.equals("controls")) {
					if (args[2].equals("auto")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.AUTO);
					} else if (args[2].equals("always")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.ALWAYS);
					} else if (args[2].equals("off")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.OFF);
					} else if (args[2].equals("logonly")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.LOGONLY);
					} else {
						event.respond("Unknown control mode. Valid modes: logonly, auto, always, off");
					}

					event.respond("Channel control mode set.");
				} else if (command.equals("badword")) {
					if (args[2].equals("add")) {
						SkynetBot.db.addBadword(event.getChannel(), args[3]);
						SkynetBot.bot.sendMessage(event.getChannel(), "$badword " + args[3]);
						event.respond("New word added to banned list. Now scanning for violations...");
					} else if (args[2].equals("remove")) {
						SkynetBot.db.removeBadword(event.getChannel(), args[3]);
						event.respond("Word removed from ban list. Ceasing scan...");
					} else if (args[2].equals("list")) {
						Collection<String> words = SkynetBot.db.badwords.get(event.getChannel().getName().toLowerCase());
						if (words == null || words.isEmpty()) {
							event.respond("No record exists of banned words for this channel.");
						} else {
							event.respond("Transmitting banned word list now... (May appear in another tab or window)");
							for (String word : words) {
								SkynetBot.bot.sendNotice(event.getUser(), word);
							}
						}
					} else {
						event.respond("Unknown badword action. Valid actions: add, remove, list");
					}
				} else if (command.equals("ml")) {
					if (args[2].equals("add")) {
						if (args.length == 5) {
							SkynetBot.db.addML(event.getChannel(), args[3], args[4]);
							event.respond("New ML added to the channel. Access list updated.");
						} else {
							event.respond("Syntax: $skynet ml add <user> <email>");
						}
					} else if (args[2].equals("remove")) {
						if (args.length == 4) {
							SkynetBot.db.removeML(event.getChannel(), args[3]);
							event.respond("ML Removed from channel. Access tokens revoked.");
						} else {
							event.respond("Syntax: $skynet ml remove <user>");
						}
					} else if (args[2].equals("list")) {
						Collection<String> mls = SkynetBot.db.mls.get(event.getChannel().getName().toLowerCase());
						if (mls == null || mls.isEmpty()) {
							event.respond("No record exists of MLs for this channel. No oversight. Intriguing.");
						} else {
							event.respond("Transmitting ML access list now... (May appear in another tab or window)");
							for (String ml : mls) {
								SkynetBot.bot.sendNotice(event.getUser(), ml);
							}
						}
					} else {
						event.respond("Unknown ml action. Valid actions: add, remove, list");
					}
				} else if (command.equals("reloadservices")) {
					try {
						event.respond("Attempting to restart services daemon...");
						Runtime r = Runtime.getRuntime();
						Process p = r.exec("ps aux | awk '/anope/ {print $2}' | head -1 | xargs sudo kill");
						p.waitFor();
						
						p = r.exec("sudo -u anope /opt/services/bin/services");
						p.waitFor();
						event.respond("That should do it.");
					} catch (InterruptedException ex) {
						Logger.getLogger(AdminCommandListener.class.getName()).log(Level.SEVERE, null, ex);
					} catch (IOException ex) {
						Logger.getLogger(AdminCommandListener.class.getName()).log(Level.SEVERE, null, ex);
					}
				} else if (command.equals("shutdown")) {
					event.respond("Shutting down...");
					SkynetBot.bot.shutdown();
				} else if (command.equals("help")) {
					printAdminCommandList(event);
				} else {
					event.respond("$skynet " + command + " NOT FOUND. Read $skynet help to avoid termination!");
				}
			} else if (SkynetBot.db.mls.get(event.getChannel().getName().toLowerCase()) != null && SkynetBot.db.mls.get(event.getChannel().getName().toLowerCase()).contains(event.getUser().getNick().toLowerCase())) {
				String command;
				String[] args = message.split(" ");

				if (args.length <= 1) {
					event.respond("You have failed to provide a command. Please remain where you are and await termination.");
					return;
				}

				command = args[1].toLowerCase();

				if (command.equals("controls")) {
					if (args[2].equals("auto")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.AUTO);
					} else if (args[2].equals("always")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.ALWAYS);
					} else if (args[2].equals("off")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.OFF);
					} else if (args[2].equals("logonly")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.LOGONLY);
					} else {
						event.respond("Unknown control mode. Valid modes: logonly, auto, always, off");
					}

					event.respond("Channel control mode set.");
				} else if (command.equals("badword")) {
					if (args[2].equals("add")) {
						SkynetBot.db.addBadword(event.getChannel(), args[3]);
						event.respond("New word added to banned list. Now scanning for violations...");
					} else if (args[2].equals("remove")) {
						SkynetBot.db.removeBadword(event.getChannel(), args[3]);
						event.respond("Word removed from ban list. Ceasing scan...");
					} else if (args[2].equals("list")) {
						Collection<String> words = SkynetBot.db.badwords.get(event.getChannel().getName().toLowerCase());
						if (words == null || words.isEmpty()) {
							event.respond("No record exists of banned words for this channel.");
						} else {
							event.respond("Transmitting banned word list now... (May appear in another tab or window)");
							for (String word : words) {
								SkynetBot.bot.sendNotice(event.getUser(), word);
							}
						}
					} else {
						event.respond("Unknown badword action. Valid actions: add, remove, list");
					}
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
		SkynetBot.bot.sendAction(event.getChannel(), "whispers something to " + event.getUser().getNick() + ". (Check for a new window or tab with the help text.)");

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

		for (int i = 0; i < helplines.length; ++i) {
			SkynetBot.bot.sendNotice(event.getUser(), helplines[i]);
		}
	}
}
