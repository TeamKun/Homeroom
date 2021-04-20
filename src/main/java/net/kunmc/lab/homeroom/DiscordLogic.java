package net.kunmc.lab.homeroom;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;

public class DiscordLogic extends ListenerAdapter {
    private JDA jda;

    private final String token;
    private final long guildId;
    private final long voiceChannelId;
    private final boolean enableUnmuteOnLeave;

    public DiscordLogic(String tokenIn, long guildIdIn, long voiceChannelIdIn, boolean enableUnmuteOnLeaveIn) {
        token = tokenIn;
        guildId = guildIdIn;
        voiceChannelId = voiceChannelIdIn;
        enableUnmuteOnLeave = enableUnmuteOnLeaveIn;
    }

    public void init() throws LoginException {
        if (jda != null)
            return;

        jda = JDABuilder.create(token, GatewayIntent.GUILD_VOICE_STATES)
                .addEventListeners(this)
                .enableCache(CacheFlag.VOICE_STATE)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                .build();
    }

    public void shutdown() {
        if (jda == null)
            return;
        jda.shutdownNow();
        jda = null;
    }

    public void setMute(long memberId, boolean muted) {
        if (jda == null)
            return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return;
        Member member = guild.getMemberById(memberId);
        if (member == null)
            return;
        member.mute(muted).queue();
    }

    public Boolean toggleMute(long memberId) {
        if (jda == null)
            return null;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return null;
        Member member = guild.getMemberById(memberId);
        if (member == null)
            return null;
        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null)
            return null;
        boolean newMuted = !voiceState.isGuildMuted();
        member.mute(newMuted).queue();
        return newMuted;
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (!enableUnmuteOnLeave)
            return;

        if (voiceChannelId == event.getOldValue().getIdLong())
            event.getMember().mute(false).queue();
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (!enableUnmuteOnLeave)
            return;

        if (voiceChannelId == event.getOldValue().getIdLong() && voiceChannelId != event.getNewValue().getIdLong())
            event.getMember().mute(false).queue();
    }
}
