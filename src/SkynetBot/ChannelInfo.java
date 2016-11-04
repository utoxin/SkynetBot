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

import java.util.ArrayList;
import org.pircbotx.Channel;

/**
 *
 * @author mwalker
 */
class ChannelInfo {
	Channel channel;
	ControlMode control;
	ArrayList<String> mls;

	enum ControlMode {
		AUTO, ALWAYS, OFF, LOGONLY
	}

	/**
	 * Construct channel with default flags.
	 *
	 * @param channel What is the name of the channel
	 */
	ChannelInfo(Channel channel) {
		this.channel = channel;
		this.control = ControlMode.AUTO;

		loadMLs();
	}

	ChannelInfo(Channel channel, ControlMode mode) {
		this.channel = channel;
		this.control = mode;

		loadMLs();
	}

	private void loadMLs() {
		mls = (ArrayList<String>) SkynetBot.db.mls.get(this.channel.getName());
	}
}
