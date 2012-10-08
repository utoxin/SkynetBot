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
		String command;
		String argsString;
		String[] args = null;

		if (message.charAt(0) == '*') {
			if (SkynetBot.db.admin_list.contains(event.getUser().getNick().toLowerCase())) {
				int space = message.indexOf(" ");
				if (space > 0) {
					command = message.substring(1, space).toLowerCase();
					argsString = message.substring(space + 1);
					args = argsString.split(" ", 0);
				} else {
					command = message.toLowerCase().substring(1);
				}

				if (command.equals("controls")) {
					if (args[0].equals("auto")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.AUTO);
					} else if (args[0].equals("always")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.ALWAYS);
					} else if (args[0].equals("off")) {
						SkynetBot.db.setChannelControlMode(event.getChannel(), ChannelInfo.ControlMode.OFF);
					} else {
						event.respond("Unknown control mode. Valid modes: auto, always, off");
					}

					event.respond("Channel control mode set.");
				} else if (command.equals("help")) {
					printAdminCommandList(event);
				} else {
					event.respond("*" + command + " is not a valid admin command - try *help");
				}
			} else {
				event.respond("You are not an admin. Only Admins have access to that command.");
			}
		}
	}
	
	private void printAdminCommandList( MessageEvent event ) {
		SkynetBot.bot.sendAction(event.getChannel(), "whispers something to " + event.getUser().getNick() + ". (Check for a new window or tab with the help text.)");

		String[] helplines = {"Core Skynet Admin Commands:",
							  "    *controls auto   - Turns on channel control mode when no other ops present",
							  "    *controls always - Turns on channel control mode all the time",
							  "    *controls off    - Turns off channel control mode",};

		for (int i = 0; i < helplines.length; ++i) {
			SkynetBot.bot.sendNotice(event.getUser(), helplines[i]);
		}
	}
}
