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

import org.pircbotx.Channel;

/**
 *
 * @author mwalker
 */
public class ChannelInfo {
	public Channel channel;
	public ControlMode control;

	public enum ControlMode {
		AUTO, ALWAYS, OFF
	}

	/**
	 * Construct channel with default flags.
	 *
	 * @param name What is the name of the channel
	 */
	public ChannelInfo( Channel channel ) {
		this.channel = channel;
		this.control = ControlMode.AUTO;
	}

	public ChannelInfo ( Channel channel, ControlMode mode ) {
		this.channel = channel;
		this.control = mode;
	}
}