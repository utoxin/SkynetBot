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
public class UserCommandListener extends ListenerAdapter  {
	@Override
	public void onMessage( MessageEvent event ) {
		String message = Colors.removeFormattingAndColors(event.getMessage());

		if (message.startsWith("!skynet")) {
			String command;
			String[] args = message.split(" ");

			if (args.length <= 1) {
				event.respond("You have failed to provide a command. Please remain where you are and await termination.");
				return;
			}

			command = args[1].toLowerCase();

			if (command.equals("lastseen")) {
				event.respond(SkynetBot.db.getLastSeen(args[2], event.getChannel()));
			} else if (command.equals("help")) {
				printCommandList(event);
			} else {
				event.respond("!skynet " + command + " NOT FOUND. Read !skynet help to avoid termination!");
			}
		}
	}
	
	private void printCommandList( MessageEvent event ) {
		SkynetBot.bot.sendAction(event.getChannel(), "whispers something to " + event.getUser().getNick() + ". (Check for a new window or tab with the help text.)");

		String[] helplines = {"Core Skynet User Commands:",
							  "    $skynet lastseen <user> - Report when that user was last seen in channel",};

		for (int i = 0; i < helplines.length; ++i) {
			SkynetBot.bot.sendNotice(event.getUser(), helplines[i]);
		}
	}
}
