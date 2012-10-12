/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SkynetBot;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.InviteEvent;

/**
 *
 * @author Matthew Walker
 */
public class ServerListener extends ListenerAdapter {

	@Override
	public void onInvite( InviteEvent event ) {
		if (!SkynetBot.db.channel_data.containsKey(event.getChannel())) {
			SkynetBot.bot.joinChannel(event.getChannel());
			SkynetBot.db.saveChannel(SkynetBot.bot.getChannel(event.getChannel()));
			SkynetBot.bot.sendRawLineNow("CHANSERV AOP " + event.getChannel() + " ADD " + SkynetBot.bot.getNick());
		}
	}
}
