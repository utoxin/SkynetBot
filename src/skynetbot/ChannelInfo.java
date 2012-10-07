/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package skynetbot;

import org.pircbotx.Channel;

/**
 *
 * @author mwalker
 */
public class ChannelInfo {
	public Channel channel;

	/**
	 * Construct channel with default flags.
	 *
	 * @param name What is the name of the channel
	 */
	public ChannelInfo( Channel channel ) {
		this.channel = channel;
	}
}
