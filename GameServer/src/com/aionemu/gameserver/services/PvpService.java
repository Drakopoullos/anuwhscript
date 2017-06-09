/*
 * This file is part of aion-unique <aion-unique.org>.
 *
 *  aion-unique is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-unique is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.PunishmentConfig;
import com.aionemu.gameserver.configs.main.PvPConfig;
import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.controllers.attack.KillList;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_ACCOUNT_TOLL_INFO;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.utils.stats.StatFunctions;
import com.aionemu.gameserver.world.World;

/**
 * @author Sarynth
 */
public class PvpService {

  private static Logger log = LoggerFactory.getLogger("KILL_LOG");
	public static final PvpService getInstance() {
		return SingletonHolder.instance;
	}

	private FastMap<Integer, KillList> pvpKillLists;

	private PvpService() {
		pvpKillLists = new FastMap<Integer, KillList>();
	}

	/**
	 * @param winnerId
	 * @param victimId
	 * @return
	 */
	private int getKillsFor(int winnerId, int victimId) {
		KillList winnerKillList = pvpKillLists.get(winnerId);

		if (winnerKillList == null)
			return 0;
		return winnerKillList.getKillsFor(victimId);
	}

	/**
	 * @param winnerId
	 * @param victimId
	 */
	private void addKillFor(int winnerId, int victimId) {
		KillList winnerKillList = pvpKillLists.get(winnerId);
		if (winnerKillList == null) {
			winnerKillList = new KillList();
			pvpKillLists.put(winnerId, winnerKillList);
		}
		winnerKillList.addKillFor(victimId);
	}

	/**
	 * @param victim
	 */
	public void doReward(Player victim) {
		// winner is the player that receives the kill count
		final Player winner = victim.getAggroList().getMostPlayerDamage();

		int totalDamage = victim.getAggroList().getTotalDamage();

		if (totalDamage == 0 || winner == null) {
			return;
		}

		// Add Player Kill to record.
			// Pvp Kill Reward.
		int reduceap = PunishmentConfig.PUNISHMENT_REDUCEAP;
		if (reduceap < 0)
			reduceap *= -1;
		if (reduceap > 100) {
			reduceap = 100;
		}

		// Announce that player has died.
			
		//Kill-log
		if ((LoggingConfig.LOG_PL) || (reduceap > 0)) {
      String ip1 = winner.getClientConnection().getIP();
      String mac1 = winner.getClientConnection().getMacAddress();
      String ip2 = victim.getClientConnection().getIP();
      String mac2 = victim.getClientConnection().getMacAddress();
      if ((mac1 != null) && (mac2 != null)) {
        if ((ip1.equalsIgnoreCase(ip2)) && (mac1.equalsIgnoreCase(mac2))) {
					AuditLogger.info(winner, "Power Leveling : " + winner.getName() + " with " + victim.getName() + ", They have the sames ip=" + ip1 + " and mac=" + mac1 + ".");
					if (reduceap > 0) {
						int win_ap = winner.getAbyssRank().getAp() * reduceap / 100;
						int vic_ap = victim.getAbyssRank().getAp() * reduceap / 100;
						AbyssPointsService.addAp(winner, -win_ap);
						AbyssPointsService.addAp(victim, -vic_ap);
						PacketSendUtility.sendMessage(winner, "[PL-AP] You lost " + reduceap + "% of your total ap");
						PacketSendUtility.sendMessage(victim, "[PL-AP] You lost " + reduceap + "% of your total ap");
        }
					return;
				}
				if (ip1.equalsIgnoreCase(ip2)) {
					AuditLogger.info(winner, "Possible Power Leveling : " + winner.getName() + " with " + victim.getName() + ", They have the sames ip=" + ip1 + ".");
					AuditLogger.info(winner, "Check if " + winner.getName() + " and " + victim.getName() + " are Brothers-Sisters-Lovers-dogs-cats...");
        }

			}
		}
		if (winner.getLevel() - victim.getLevel() <= PvPConfig.MAX_AUTHORIZED_LEVEL_DIFF) {
			if (getKillsFor(winner.getObjectId().intValue(), victim.getObjectId().intValue()) < PvPConfig.CHAIN_KILL_NUMBER_RESTRICTION) {
				if (PvPConfig.ENABLE_MEDAL_REWARDING) {
					if (Rnd.get() * 100 < PvPRewardService.getMedalRewardChance(winner, victim)) {
						int medalId = PvPRewardService.getRewardId(winner, victim, false);
						long medalCount = PvPRewardService.getRewardQuantity(winner, victim);
						ItemService.addItem(winner, medalId, medalCount);
						if (winner.getInventory().getItemCountByItemId(medalId) > 0) {
							if (medalCount == 1)
								PacketSendUtility.sendPacket(winner, new SM_SYSTEM_MESSAGE(1390000, new DescriptionId(medalId)));
							else {
								PacketSendUtility.sendPacket(winner, new SM_SYSTEM_MESSAGE(1390005, medalCount, new DescriptionId(medalId)));
							}
						}
					}
				}
				if ((PvPConfig.ENABLE_TOLL_REWARDING) && (Rnd.get() * 100.0F < PvPRewardService.getTollRewardChance(winner, victim))) {
					int qt = PvPRewardService.getTollQuantity(winner, victim);
					InGameShopEn.getInstance().addToll(winner, qt);
					if (qt == 1)
						PacketSendUtility.sendBrightYellowMessage(winner, "You obtained " + qt + " point toll.");
					else {
						PacketSendUtility.sendBrightYellowMessage(winner, "You obtained " + qt + " point toll.");
					}
				}
				
				// Reward Pk Kill
				if (victim.isBandit()) {
					long toll = winner.getClientConnection().getAccount().getToll() + PvPConfig.TOLL_PK_COST;
					
					if (LoginServer.getInstance().sendPacket(new SM_ACCOUNT_TOLL_INFO(toll, winner.getClientConnection().getAccount().getLuna(), winner.getAcountName()))) {
						winner.getClientConnection().getAccount().setToll(toll);
						PacketSendUtility.sendMessage(winner, "You've received " + PvPConfig.TOLL_PK_COST + " tolls from MAD bunny!");
					}
					
					Iterator<Player> iter = World.getInstance().getPlayersIterator();
					String message = victim.getName() +"'s madness ended by " + winner.getName();
					Player target;
					while (iter.hasNext()) {
						target = iter.next();
						PacketSendUtility.sendBrightYellowMessageOnCenter(target, message);
					}
					
				}
				
				if (PvPConfig.GENOCIDE_SPECIAL_REWARDING != 0) {
					switch (PvPConfig.GENOCIDE_SPECIAL_REWARDING) {
						case 1:
							if ((winner.getSpreeLevel() <= 2) || (Rnd.get() * 100 >= PvPConfig.SPECIAL_REWARD_CHANCE))
								break;
							int abyssId = PvPRewardService.getRewardId(winner, victim, true);
							ItemService.addItem(winner, abyssId, 1L);
							log.info("[PvP][Advanced] {Player : " + winner.getName() + "} has won " + abyssId + " for killing {Player : " + victim.getName() + "}");
							break;
						default:
							if ((winner.getSpreeLevel() <= 2) || (Rnd.get() * 100 >= PvPConfig.SPECIAL_REWARD_CHANCE))
								break;
							ItemService.addItem(winner, PvPConfig.GENOCIDE_SPECIAL_REWARDING, 1L);
							log.info("[PvP][Advanced] {Player : " + winner.getName() + "} has won " + PvPConfig.GENOCIDE_SPECIAL_REWARDING + " for killing {Player : " + victim.getName() + "}");
							break;
					}
				}
			}
			else {
				PacketSendUtility.sendMessage(winner, "You will not gain anything by killing the player " + victim.getName() + " for the rest of the day because you kill too many times.");
			}
		}
		int playerDamage = 0;
		boolean success = false;

		// Distribute AP to groups and players that had damage.
		for (AggroInfo aggro : victim.getAggroList().getFinalDamageList(true)) {
			if (aggro.getAttacker() instanceof Player) {
				success = rewardPlayer(victim, totalDamage, aggro);
			}
			else if (aggro.getAttacker() instanceof PlayerGroup) {
				success = rewardPlayerGroup(victim, totalDamage, aggro);
			}
			else if (aggro.getAttacker() instanceof PlayerAlliance) {
				success = rewardPlayerAlliance(victim, totalDamage, aggro);
			}

			// Add damage last, so we don't include damage from same race. (Duels, Arena)
			if (success)
				playerDamage += aggro.getDamage();
		}
		ProtectorConquerorService.getInstance().updateRanks(winner, victim);

		// Apply lost AP to defeated player
		final int apLost = StatFunctions.calculatePvPApLost(victim, winner);
		final int apActuallyLost = (int) (apLost * playerDamage / totalDamage);

		if (apActuallyLost > 0) {
			AbyssPointsService.addAp(victim, -apActuallyLost);

		}
		// Apply lost GP to defeated player
        final int gpLost = StatFunctions.calculatePvPGpLost(victim, winner);
        final int gpActuallyLost = (int) (gpLost * playerDamage / totalDamage);

        if (gpActuallyLost > 0) {
            AbyssPointsService.addGp(victim, -gpActuallyLost);
        }
		PacketSendUtility.broadcastPacketAndReceive(victim, SM_SYSTEM_MESSAGE.STR_MSG_COMBAT_FRIENDLY_DEATH_TO_B(victim.getName(), winner.getName()));
		if (LoggingConfig.LOG_KILL)
			log.info("[KILL] Player [" + winner.getName() + "] killed [" + victim.getName() + "]");
	}

	/**
	 * @param victim
	 * @param totalDamage
	 * @param aggro
	 * @return true if group is not same race
	 */
	private boolean rewardPlayerGroup(Player victim, int totalDamage, AggroInfo aggro) {
		// Reward Group
		PlayerGroup group = ((PlayerGroup) aggro.getAttacker());

		// Don't Reward Player of Same Faction.
		if (group.getRace() == victim.getRace() && !victim.isBandit())
			return false;

		// Find group members in range
		List<Player> players = new ArrayList<Player>();

		// Find highest rank and level in local group
		int maxRank = AbyssRankEnum.GRADE9_SOLDIER.getId();
		int maxLevel = 0;

		for (Player member : group.getMembers()) {
			if (MathUtil.isIn3dRange(member, victim, GroupConfig.GROUP_MAX_DISTANCE)) {
				// Don't distribute AP to a dead player!
				if (!member.getLifeStats().isAlreadyDead()) {
					players.add(member);
					if (member.getLevel() > maxLevel)
						maxLevel = member.getLevel();
					if (member.getAbyssRank().getRank().getId() > maxRank)
						maxRank = member.getAbyssRank().getRank().getId();
				}
			}
		}

		// They are all dead or out of range.
    if (players.isEmpty())
			return false;
		
		int baseApReward = StatFunctions.calculatePvpApGained(victim, maxRank, maxLevel);
		int baseGpReward = StatFunctions.calculatePvpGpGained(victim, maxRank, maxLevel);
		int baseXpReward = StatFunctions.calculatePvpXpGained(victim, maxRank, maxLevel);
		int baseDpReward = StatFunctions.calculatePvpDpGained(victim, maxRank, maxLevel);
		float groupPercentage = (float) aggro.getDamage() / totalDamage;
		int apRewardPerMember = Math.round(baseApReward * groupPercentage / players.size());
		int gpRewardPerMember = Math.round(baseGpReward * groupPercentage / players.size());
		int xpRewardPerMember = Math.round(baseXpReward * groupPercentage / players.size());
		int dpRewardPerMember = Math.round(baseDpReward * groupPercentage / players.size());
		
		for (Player member : players) {
			int memberApGain = 1;
			int memberGpGain = 1;
			int memberXpGain = 1;
			int memberDpGain = 1;
			if (this.getKillsFor(member.getObjectId(), victim.getObjectId()) < PvPConfig.CHAIN_KILL_NUMBER_RESTRICTION) {
				if (apRewardPerMember > 0)
					memberApGain = Math.round(apRewardPerMember * member.getRates().getApPlayerGainRate());
				if (gpRewardPerMember > 0)
                    memberGpGain = Math.round(RewardType.GP_PLAYER.calcReward(member, gpRewardPerMember));
				if (xpRewardPerMember > 0)
					memberXpGain = Math.round(xpRewardPerMember * member.getRates().getXpPlayerGainRate());
				if (dpRewardPerMember > 0)
					memberDpGain = Math.round(StatFunctions.adjustPvpDpGained(dpRewardPerMember, victim.getLevel(), member.getLevel()) * member.getRates().getDpPlayerRate());
				member.getAbyssRank().updateKillCounts();
				
				if (PvPConfig.ENABLE_KILLING_SPREE_SYSTEM) {
					Player luckyPlayer = (Player) players.get(Rnd.get(players.size()));
					PvPSpreeService.increaseRawKillCount(luckyPlayer);
				}
			}
			Player partner = member.findPartner();
			if (member.isMarried() && member.getPlayerGroup2().getMembers() == partner && member.getPlayerGroup2().getMembers().size() == 2) {
				AbyssPointsService.addAp(member, victim, memberApGain + (memberApGain * 20 / 100));
                AbyssPointsService.addGp(member, victim, memberGpGain + (memberGpGain * 20 / 100));	//20% more AP and Gp for weddings
			} else {
				AbyssPointsService.addAp(member, victim, memberApGain);
				AbyssPointsService.addGp(member, victim, memberGpGain);
			}
			member.getCommonData().addExp(memberXpGain, RewardType.PVP_KILL, victim.getName());
			member.getCommonData().addDp(memberDpGain);
			this.addKillFor(member.getObjectId(), victim.getObjectId());
			//notify Kill-Quests
			int worldId = member.getWorldId();
			QuestEngine.getInstance().onKillInWorld(new QuestEnv(victim, member, 0, 0), worldId);
			QuestEngine.getInstance().onKillRanked(new QuestEnv(victim, member, 0, 0), victim.getAbyssRank().getRank());
		}

		return true;
	}

	/**
	 * @param victim
	 * @param totalDamage
	 * @param aggro
	 * @return true if group is not same race
	 */
	private boolean rewardPlayerAlliance(Player victim, int totalDamage, AggroInfo aggro) {
		// Reward Alliance
		PlayerAlliance alliance = ((PlayerAlliance) aggro.getAttacker());

		// Don't Reward Player of Same Faction.
		if (alliance.getLeaderObject().getRace() == victim.getRace() && !victim.isBandit())
			return false;

		// Find group members in range
		List<Player> players = new ArrayList<Player>();

		// Find highest rank and level in local group
		int maxRank = AbyssRankEnum.GRADE9_SOLDIER.getId();
		int maxLevel = 0;

		for (Player member : alliance.getMembers()) {
			if (!member.isOnline())
				continue;
			if (MathUtil.isIn3dRange(member, victim, GroupConfig.GROUP_MAX_DISTANCE)) {
				// Don't distribute AP to a dead player!
				if (!member.getLifeStats().isAlreadyDead()) {
					players.add(member);
					if (member.getLevel() > maxLevel)
						maxLevel = member.getLevel();
					if (member.getAbyssRank().getRank().getId() > maxRank)
						maxRank = member.getAbyssRank().getRank().getId();
				}
			}
		}

		// They are all dead or out of range.
    if (players.isEmpty())
			return false;

		int baseApReward = StatFunctions.calculatePvpApGained(victim, maxRank, maxLevel);
		int baseGpReward = StatFunctions.calculatePvpGpGained(victim, maxRank, maxLevel);
		int baseXpReward = StatFunctions.calculatePvpXpGained(victim, maxRank, maxLevel);
		int baseDpReward = StatFunctions.calculatePvpDpGained(victim, maxRank, maxLevel);
		float groupPercentage = (float) aggro.getDamage() / totalDamage;
		int apRewardPerMember = Math.round(baseApReward * groupPercentage / players.size());
		int gpRewardPerMember = Math.round(baseGpReward * groupPercentage / players.size());
		int xpRewardPerMember = Math.round(baseXpReward * groupPercentage / players.size());
		int dpRewardPerMember = Math.round(baseDpReward * groupPercentage / players.size());
		
		for (Player member : players) {
			int memberApGain = 1;
			int memberGpGain = 1;
			int memberXpGain = 1;
			int memberDpGain = 1;
			if (this.getKillsFor(member.getObjectId(), victim.getObjectId()) < PvPConfig.CHAIN_KILL_NUMBER_RESTRICTION) {
				if (apRewardPerMember > 0)
					memberApGain = Math.round(apRewardPerMember * member.getRates().getApPlayerGainRate());
				if (gpRewardPerMember > 0)
                    memberGpGain = Math.round(RewardType.GP_PLAYER.calcReward(member, gpRewardPerMember));
				if (xpRewardPerMember > 0)
					memberXpGain = Math.round(xpRewardPerMember * member.getRates().getXpPlayerGainRate());
				if (dpRewardPerMember > 0)
					memberDpGain = Math.round(StatFunctions.adjustPvpDpGained(dpRewardPerMember, victim.getLevel(), member.getLevel()) * member.getRates().getDpPlayerRate());
				member.getAbyssRank().updateKillCounts();
				if (PvPConfig.ENABLE_KILLING_SPREE_SYSTEM) {
					Player luckyPlayer = (Player) players.get(Rnd.get(players.size()));
					PvPSpreeService.increaseRawKillCount(luckyPlayer);
				}
			}
			AbyssPointsService.addAp(member, victim, memberApGain);
			AbyssPointsService.addGp(member, victim, memberGpGain);
			member.getCommonData().addExp(memberXpGain, RewardType.PVP_KILL, victim.getName());
			member.getCommonData().addDp(memberDpGain);
			
			this.addKillFor(member.getObjectId(), victim.getObjectId());
			//notify Kill-Quests
			int worldId = member.getWorldId();
			QuestEngine.getInstance().onKillInWorld(new QuestEnv(victim, member, 0, 0), worldId);
			QuestEngine.getInstance().onKillRanked(new QuestEnv(victim, member, 0, 0), victim.getAbyssRank().getRank());
		}

		return true;
	}

	/**
	 * @param victim
	 * @param totalDamage
	 * @param aggro
	 * @return true if player is not same race
	 */
	private boolean rewardPlayer(Player victim, int totalDamage, AggroInfo aggro) {
		// Reward Player
		Player winner = ((Player) aggro.getAttacker());
		
    if ((winner.getRace() == victim.getRace() && !victim.isBandit() && !winner.isBandit()) || (!MathUtil.isIn3dRange(winner, victim, GroupConfig.GROUP_MAX_DISTANCE)) || (winner.getLifeStats().isAlreadyDead())) {
			return false;
    }
		int baseApReward = 1;
		int baseGpReward = 1;
		int baseXpReward = 1;
		int baseDpReward = 1;

		if (this.getKillsFor(winner.getObjectId(), victim.getObjectId()) < PvPConfig.CHAIN_KILL_NUMBER_RESTRICTION) {
			baseApReward = StatFunctions.calculatePvpApGained(victim, winner.getAbyssRank().getRank().getId(), winner.getLevel());
			baseGpReward = StatFunctions.calculatePvpGpGained(victim, winner.getAbyssRank().getRank().getId(), winner.getLevel());
			baseXpReward = StatFunctions.calculatePvpXpGained(victim, winner.getAbyssRank().getRank().getId(), winner.getLevel());
			baseDpReward = StatFunctions.calculatePvpDpGained(victim, winner.getAbyssRank().getRank().getId(), winner.getLevel());
			winner.getAbyssRank().updateKillCounts();
			if (PvPConfig.ENABLE_KILLING_SPREE_SYSTEM) {
				PvPSpreeService.increaseRawKillCount(winner);
			}
			if (EventsConfig.ENABLE_CRAZY) {
				if (winner.getRace() != victim.getRace())
					CrazyDaevaService.getInstance().increaseRawKillCount(winner);
			}
		}

		int apPlayerReward = Math.round(baseApReward * winner.getRates().getApPlayerGainRate() * aggro.getDamage() / totalDamage);
		int gpPlayerReward = Math.round(baseGpReward * aggro.getDamage() / totalDamage);
		gpPlayerReward = (int) RewardType.GP_PLAYER.calcReward(winner, gpPlayerReward);
		int xpPlayerReward = Math.round(baseXpReward * winner.getRates().getXpPlayerGainRate() * aggro.getDamage() / totalDamage);
		int dpPlayerReward = Math.round(baseDpReward * winner.getRates().getDpPlayerRate() * aggro.getDamage() / totalDamage);

		AbyssPointsService.addAp(winner, victim, apPlayerReward);
		AbyssPointsService.addGp(winner, victim, gpPlayerReward);
		winner.getCommonData().addExp(xpPlayerReward, RewardType.PVP_KILL, victim.getName());
		winner.getCommonData().addDp(dpPlayerReward);
		this.addKillFor(winner.getObjectId(), victim.getObjectId());
		//notify Kill-Quests
		int worldId = winner.getWorldId();
		QuestEngine.getInstance().onKillInWorld(new QuestEnv(victim, winner, 0, 0), worldId);
		QuestEngine.getInstance().onKillRanked(new QuestEnv(victim, winner, 0, 0), victim.getAbyssRank().getRank());

		return true;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final PvpService instance = new PvpService();
	}
}
