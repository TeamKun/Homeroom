package net.kunmc.lab.homeroom;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;

public class DiscordLogic extends ListenerAdapter {
    private JDA jda;
    private long id;

    public void init(String token) throws LoginException {
        jda = JDABuilder.create(token, GatewayIntent.GUILD_VOICE_STATES)
                .addEventListeners(this)
                .build();
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (id == event.getOldValue().getIdLong())
            event.getMember().mute(false).queue();
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (id == event.getOldValue().getIdLong() && id != event.getNewValue().getIdLong())
            event.getMember().mute(false).queue();
    }
}
