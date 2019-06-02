package com.sx4.core;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jockie.bot.core.command.factory.impl.MethodCommandFactory;
import com.jockie.bot.core.command.impl.CommandListener;
import com.jockie.bot.core.command.impl.CommandStore;
import com.jockie.bot.core.command.manager.impl.ContextManagerFactory;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlRuntimeError;
import com.rethinkdb.net.Connection;
import com.sx4.cache.ChangesMessageCache;
import com.sx4.cache.SteamCache;
import com.sx4.events.AntiInviteEvents;
import com.sx4.events.AntiLinkEvents;
import com.sx4.events.AutoroleEvents;
import com.sx4.events.AwaitEvents;
import com.sx4.events.ConnectionEvents;
import com.sx4.events.GiveawayEvents;
import com.sx4.events.ImageModeEvents;
import com.sx4.events.ModEvents;
import com.sx4.events.MuteEvents;
import com.sx4.events.ReminderEvents;
import com.sx4.events.SelfroleEvents;
import com.sx4.events.ServerLogEvents;
import com.sx4.events.ServerPostEvents;
import com.sx4.events.StatsEvents;
import com.sx4.events.StatusEvents;
import com.sx4.events.TriggerEvents;
import com.sx4.events.WelcomerEvents;
import com.sx4.logger.handler.EventHandler;
import com.sx4.logger.handler.ExceptionHandler;
import com.sx4.logger.handler.GuildMessageCache;
import com.sx4.settings.Settings;
import com.sx4.utils.CheckUtils;
import com.sx4.utils.DatabaseUtils;
import com.sx4.utils.HelpUtils;
import com.sx4.utils.ModUtils;
import com.sx4.utils.TimeUtils;

import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.handle.GuildSetupController;
import okhttp3.OkHttpClient;

public class Sx4Bot {
	
	static {
		if (!Charset.defaultCharset().equals(StandardCharsets.UTF_8)) {
			System.setProperty("file.encoding", "UTF-8");
			Field charset = null;
			try {
				charset = Charset.class.getDeclaredField("defaultCharset");
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
			charset.setAccessible(true);
			try {
				charset.set(null, null);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final EventWaiter waiter = new EventWaiter();
	
	public static OkHttpClient client = new OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.callTimeout(5, TimeUnit.SECONDS)
			.build();
	
	public static ScheduledExecutorService scheduledExectuor = Executors.newSingleThreadScheduledExecutor();
	
	private static ShardManager bot;
	
	private static CommandListener listener;
	
	private static Connection connection;
	
	private static EventHandler eventHandler;
	
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

	public static void main(String[] args) throws Exception {	
		SteamCache.getGames();
		
		connection = RethinkDB.r
			    .connection()
			    .connect();
		
		try {
			RethinkDB.r.dbCreate(Settings.DATABASE_NAME).run(connection);
		} catch(ReqlRuntimeError e) {}
		
		connection.use(Settings.DATABASE_NAME);
		
		eventHandler = new EventHandler(connection);
		
		DatabaseUtils.ensureTables("antiad", "antilink", "auction", "autorole", "await", "bank", "blacklist", 
				"botstats", "fakeperms", "giveaway", "imagemode", "logs", "marriage", "modlogs", "mute", 
				"offence", "prefix", "reactionrole", "reminders", "rps", "selfroles", "stats", "suggestions", 
				"tax", "triggers", "userprofile", "warn", "welcomer");
		
		ContextManagerFactory.getDefault().registerContext(Connection.class, (event, type) -> connection);
		
		MethodCommandFactory.setDefault(new Sx4CommandFactory());
		
		listener = new CommandListener()
				.addCommandStore(CommandStore.of("com.sx4.modules"))
				.addDevelopers(402557516728369153L, 190551803669118976L)
				.setDefaultPrefixes("s?", "sx4 ", "S?")
				.setHelpFunction((event, prefix, failures) -> {
					if (CheckUtils.canReply(event, prefix)) {
						Member self = event.getGuild().getMember(event.getJDA().getSelfUser());
						if (self.hasPermission(Permission.MESSAGE_EMBED_LINKS)) {
							event.getTextChannel().sendMessage(HelpUtils.getHelpMessage(failures.get(0).getCommand())).queue();
						} else {
							event.getTextChannel().sendMessage("I am missing the permission `Embed Links`, therefore I cannot show you the help menu for `" + failures.get(0).getCommand().getCommandTrigger() + "` :no_entry:").queue();
						}
					}
				})
				.setCooldownFunction((event, cooldown) -> {
					if (CheckUtils.canReply(event.getMessage(), event.getPrefix())) {
						event.reply("Slow down there! You can execute this command again in " + TimeUtils.toTimeString(cooldown.getTimeRemainingMillis(), ChronoUnit.MILLIS) + " :stopwatch:").queue(); 
					}
				})
				.setMissingPermissionExceptionFunction((event, permission) -> {
					if (CheckUtils.canReply(event.getMessage(), event.getPrefix())) {
						event.reply("I am missing the permission `" + permission.getName() + "`, therefore I cannot execute that command :no_entry:").queue();
					}
				})
				.setMissingPermissionFunction((event, permissions) -> {
					if (CheckUtils.canReply(event.getMessage(), event.getPrefix())) {
						List<String> permissionNames = new ArrayList<String>();
						for (Permission permission : permissions) {
							permissionNames.add(permission.getName());
						}
						
						event.reply("I am missing the permission" + (permissions.size() == 1 ? "" : "s") + " `" + String.join("`, `", permissionNames) + "`, therefore I cannot execute that command :no_entry:").queue();
					}
				});
		
		listener.removeDefaultPreExecuteChecks()
				.addPreExecuteCheck((event, command) -> {
					return CheckUtils.checkBlacklist(event, connection);
				})
				.addPreExecuteCheck((event, command) -> CheckUtils.canReply(event.getMessage(), event.getPrefix()))
				.addPreExecuteCheck((event, command) -> {
					if (command instanceof Sx4Command) {
						Sx4Command sx4Command = ((Sx4Command) command);
						if (sx4Command.isDonator()) {
							Guild guild = event.getShardManager().getGuildById(Settings.SUPPORT_SERVER_ID);
							Role donatorRole = guild.getRoleById(Settings.DONATOR_ONE_ROLE_ID);
				
							Member member = guild.getMemberById(event.getAuthor().getIdLong());
				
							if (member != null) {
							    if (!event.getGuild().getMembersWithRoles(donatorRole).contains(member)) {
							    	event.reply("You need to be a donator to execute this command :no_entry:").queue();
							    	return false;
							    }
							}
						}
					}
					
					return true;
				})
				.addPreExecuteCheck(listener.defaultBotPermissionCheck)
				.addPreExecuteCheck(listener.defaultNsfwCheck)
				.addPreExecuteCheck((event, command) -> {
					return CheckUtils.checkPermissions(event, connection, command.getAuthorDiscordPermissions().toArray(new Permission[0]), true);
				});
		
		listener.setPrefixesFunction(event -> ModUtils.getPrefixes(event.getGuild(), event.getAuthor()));
		listener.addCommandEventListener(new Sx4CommandEventListener());
		
		bot = new DefaultShardManagerBuilder().setToken(Settings.BOT_OATH).build();
		bot.addEventListener(listener, waiter);
		bot.addEventListener(new ChangesMessageCache(), new SelfroleEvents(), new ModEvents(), new ConnectionEvents(), new AwaitEvents(), new StatsEvents(), new WelcomerEvents(), new AutoroleEvents(), 
				new TriggerEvents(), new ImageModeEvents(), new MuteEvents(), new AntiInviteEvents(), new AntiLinkEvents(), new ServerLogEvents(), Sx4Bot.eventHandler, new ExceptionHandler(), GuildMessageCache.INSTANCE);
		
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			System.err.println("[Uncaught]");
			
			exception.printStackTrace();
			
			Sx4CommandEventListener.sendErrorMessage(bot.getGuildById(Settings.SUPPORT_SERVER_ID).getTextChannelById(Settings.ERRORS_CHANNEL_ID), exception, new Object[0]);
		});
		
		for(JDA shard : bot.getShards()) {
		    shard.awaitReady();
		}

		int availableGuilds = bot.getGuilds().size();
		int unavailableGuilds = bot.getShards().stream()
		        .mapToInt(jda -> ((JDAImpl) jda).getGuildSetupController().getSetupNodes(GuildSetupController.Status.UNAVAILABLE).size())
		        .sum();

		System.out.println(String.format("Connected to %s with %,d/%,d available servers and %,d users", bot.getShards().get(0).getSelfUser().getAsTag(), availableGuilds, availableGuilds + unavailableGuilds, bot.getUsers().size()));

		DatabaseUtils.ensureTableData();
		StatusEvents.initialize();
		ServerPostEvents.initializePosting();
		MuteEvents.ensureMuteRoles();
		StatsEvents.initializeBotLogs();
		StatsEvents.initializeGuildStats();
		ReminderEvents.ensureReminders();
		GiveawayEvents.ensureGiveaways();
		MuteEvents.ensureMutes();
		AutoroleEvents.ensureAutoroles();
		
		System.gc();
	}
	
	public static Connection getConnection() {
		return connection;
	}
	
	public static ShardManager getShardManager() {
		return bot;
	}
	
	public static CommandListener getCommandListener() {
		return listener;
	}
	
	public static EventHandler getEventHandler() {
		return eventHandler;
	}
	
	public static DateTimeFormatter getTimeFormatter() {
		return TIME_FORMATTER;
	}
}
