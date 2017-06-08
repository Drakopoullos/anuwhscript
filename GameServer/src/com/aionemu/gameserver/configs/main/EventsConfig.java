package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

public class EventsConfig
{
	@Property(key = "gameserver.event.enable", defaultValue = "false")
	public static boolean EVENT_ENABLED;
	@Property(key = "gameserver.enable.decor", defaultValue = "0")
    public static int ENABLE_DECOR;
	@Property(key = "gameserver.events.give.juice", defaultValue = "160009017")
	public static int EVENT_GIVE_JUICE;
	@Property(key = "gameserver.events.give.cake", defaultValue = "160010073")
	public static int EVENT_GIVE_CAKE;
	@Property(key = "gameserver.event.service.enable", defaultValue = "false")
	public static boolean ENABLE_EVENT_SERVICE;
	
	//VIP Tickets.
	@Property(key = "gameserver.vip.tickets.enable", defaultValue = "false")
	public static boolean ENABLE_VIP_TICKETS;
	@Property(key = "gameserver.vip.tickets.time", defaultValue = "60")
	public static int VIP_TICKETS_PERIOD;
	
	//Event Awake [Event JAP]
	@Property(key = "gameserver.event.awake.enable", defaultValue = "false")
	public static boolean ENABLE_AWAKE_EVENT;
	@Property(key = "gameserver.event.seed.transformation.time", defaultValue = "60")
	public static int SEED_TRANSFORMATION_PERIOD;
	
	//EVENT
	//Shugo Imperial Tomb 4.3
	@Property(key = "gameserver.shugo.imperial.tomb.enable", defaultValue = "true")
	public static boolean IMPERIAL_TOMB_ENABLE;
	@Property(key = "gameserver.shugo.imperial.tomb.timer.from.start.to.end", defaultValue = "10")
	public static long IMPERIAL_TOMB_TIMER;
	@Property(key = "gameserver.shugo.imperial.tomb.time.to.start", defaultValue = "0 0 0,12,20,0 ? * *")
	public static String IMPERIAL_TOMB_TIMES;
	
	//Crazy Daeva.
	@Property(key = "gameserver.crazy.daeva.enable", defaultValue = "false")
	public static boolean ENABLE_CRAZY;
	@Property(key = "gameserver.crazy.daeva.tag", defaultValue = "<Crazy>")
	public static String CRAZY_TAG;
	@Property(key = "gameserver.crazy.daeva.lowest.rnd", defaultValue = "10")
	public static int CRAZY_LOWEST_RND;
	@Property(key = "gameserver.crazy.daeva.time.to.start", defaultValue = "0 0 0,12,20,0 ? * *")
	public static String CRAZY_TIMES;
	@Property(key = "gameserver.crazy.daeva.endtime", defaultValue = "5")
	public static int CRAZY_ENDTIME;
	
	@Property(key = "gameserver.atreian.passport.enable", defaultValue = "false")
	public static boolean ENABLE_ATREIAN_PASSPORT;
}