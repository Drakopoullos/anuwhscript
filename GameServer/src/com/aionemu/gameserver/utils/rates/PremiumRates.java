package com.aionemu.gameserver.utils.rates;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.RateConfig;

public class PremiumRates extends Rates {
	
	int holidayRate = HolidayRates.getHolidayRate(0);
	
	@Override
	public float getGroupXpRate() {
		return RateConfig.PREMIUM_GROUPXP_RATE + holidayRate;
	}
	
	@Override
	public float getApNpcRate() {
		return RateConfig.PREMIUM_AP_NPC_RATE + holidayRate;
	}
	
	@Override
	public float getApPlayerGainRate() {
		return RateConfig.PREMIUM_AP_PLAYER_GAIN_RATE + holidayRate;
	}
	
	@Override
	public float getXpPlayerGainRate() {
		return RateConfig.PREMIUM_XP_PLAYER_GAIN_RATE + holidayRate;
	}
	
	@Override
	public float getGpPlayerGainRate() {
		return RateConfig.PREMIUM_GP_PLAYER_GAIN_RATE;
	}
	
	@Override
	public float getApPlayerLossRate() {
		return RateConfig.PREMIUM_AP_PLAYER_LOSS_RATE + holidayRate;
	}
	
	@Override
    public float getGpPlayerLossRate() {
        return RateConfig.PREMIUM_GP_PLAYER_LOSS_RATE;
    }
	
	@Override
	public float getDropRate() {
		return RateConfig.PREMIUM_DROP_RATE + holidayRate;
	}
	
	@Override
	public float getQuestKinahRate() {
		return RateConfig.PREMIUM_QUEST_KINAH_RATE + holidayRate;
	}
	
	@Override
	public float getQuestXpRate() {
		return RateConfig.PREMIUM_QUEST_XP_RATE + holidayRate;
	}
	
	@Override
	public float getQuestApRate() {
		return RateConfig.PREMIUM_QUEST_AP_RATE + holidayRate;
	}
	
	@Override
	public float getQuestGpRate() {
		return RateConfig.PREMIUM_QUEST_GP_RATE + holidayRate;
	}
	
	@Override
	public float getQuestAbyssOpRate() {
		return RateConfig.PREMIUM_QUEST_ABYSS_OP_RATE + holidayRate;
	}
	
	@Override
	public float getQuestExpBoostRate() {
		return RateConfig.PREMIUM_QUEST_EXP_BOOST_RATE + holidayRate;
	}
	
	@Override
	public float getXpRate() {
		return RateConfig.PREMIUM_XP_RATE + holidayRate;
	}
	
	@Override
	public float getCraftingXPRate() {
		return RateConfig.PREMIUM_CRAFTING_XP_RATE;
	}
	
	@Override
	public float getGatheringXPRate() {
		return RateConfig.PREMIUM_GATHERING_XP_RATE;
	}
	
	@Override
	public int getGatheringCountRate() {
		return RateConfig.PREMIUM_GATHERING_COUNT_RATE;
	}
	
	@Override
	public float getDpNpcRate() {
		return RateConfig.PREMIUM_DP_NPC_RATE;
	}
	
	@Override
	public float getDpPlayerRate() {
		return RateConfig.PREMIUM_DP_PLAYER_RATE;
	}
	
	@Override
	public int getCraftCritRate() {
		return CraftConfig.PREMIUM_CRAFT_CRIT_RATE;
	}
	
	@Override
	public int getComboCritRate() {
		return CraftConfig.PREMIUM_CRAFT_COMBO_RATE;
	}
	
	@Override
	public float getDisciplineRewardRate() {
		return RateConfig.PREMIUM_PVP_ARENA_DISCIPLINE_REWARD_RATE;
	}
	
	@Override
	public float getChaosRewardRate() {
		return RateConfig.PREMIUM_PVP_ARENA_CHAOS_REWARD_RATE;
	}
	
	@Override
	public float getHarmonyRewardRate() {
		return RateConfig.PREMIUM_PVP_ARENA_HARMONY_REWARD_RATE;
	}
	
	@Override
	public float getGloryRewardRate() {
		return RateConfig.PREMIUM_PVP_ARENA_GLORY_REWARD_RATE;
	}
	
	@Override
	public float getTollRewardRate() {
	    return RateConfig.PREMIUM_TOLL_REWARD_RATE;
	}
}