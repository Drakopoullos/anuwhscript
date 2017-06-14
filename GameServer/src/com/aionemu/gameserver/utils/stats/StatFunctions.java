package com.aionemu.gameserver.utils.stats;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.FallDamageConfig;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.AttackerCriticalStatus;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Homing;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.WeaponStats;
import com.aionemu.gameserver.model.templates.npc.NpcRating;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Preconditions;

public class StatFunctions
{
	private static SkillElement elements = null;
	private static PlayerClass playerClass;
	public static PlayerClass getPlayerClass() {
		return playerClass;
	}
	
	/**
	 * @param player
	 * @param target
	 * @return "XP Solo" reward from target
	 */
	public static long calculateSoloExperienceReward(Player player, Creature target) {
		int playerLevel = player.getCommonData().getLevel();
		int targetLevel = target.getLevel();
		int baseXP = ((Npc) target).getObjectTemplate().getStatsTemplate().getMaxXp();
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - playerLevel);
		float rate = player.getRates().getXpRate();
		return (int) Math.floor(baseXP * xpPercentage * rate / 100);
	}
	
	/**
	 * @param player
	 * @param target
	 * @return "XP Group" reward from target
	 */
	public static long calculateGroupExperienceReward(int maxLevelInRange, Creature target) {
		int targetLevel = target.getLevel();
		int baseXP = ((Npc) target).getObjectTemplate().getStatsTemplate().getMaxXp();
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - maxLevelInRange);
		return (int) Math.floor(baseXP * xpPercentage / 100);
	}
	
	/**
	 * @param player
	 * @param target
	 * @return DP reward from target
	 */

	public static int calculateSoloDPReward(Player player, Creature target) {
		int playerLevel = player.getCommonData().getLevel();
		int targetLevel = target.getLevel();
		NpcRating npcRating = ((Npc) target).getObjectTemplate().getRating();
		int baseDP = targetLevel * calculateRatingMultipler(npcRating);
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - playerLevel);
		float rate = player.getRates().getDpNpcRate();
		return (int) Math.floor(baseDP * xpPercentage * rate / 100);
	}
	
	/**
	 * @param player
	 * @param target
	 * @return AP reward
	 */
	public static int calculatePvEApGained(Player player, Creature target) {
		float apPercentage = target instanceof SiegeNpc ? 100f : APRewardEnum.apReward(player.getAbyssRank().getRank().getId());
		boolean lvlDiff = player.getCommonData().getLevel() - target.getLevel() > 10;
		float apNpcRate = ApNpcRating(((Npc) target).getObjectTemplate().getRating());
		return (int) (lvlDiff ? 1 : RewardType.AP_NPC.calcReward(player, (int) Math.floor(15 * apPercentage * apNpcRate / 100)));
	}
	
	/**
	 * @param defeated
	 * @param winner
	 * @return Points Lost in PvP Death
	 */
	public static int calculatePvPApLost(Player defeated, Player winner) {
		int pointsLost = Math.round(defeated.getAbyssRank().getRank().getPointsLost() * defeated.getRates().getApPlayerLossRate());
		int difference = winner.getLevel() - defeated.getLevel();
		if (difference > 4) {
			pointsLost = Math.round(pointsLost * 0.1f);
		} else {
			switch (difference) {
				case 3:
					pointsLost = Math.round(pointsLost * 0.85f);
				break;
				case 4:
					pointsLost = Math.round(pointsLost * 0.65f);
				break;
			}
		}
		return pointsLost;
	}
	
	/**
	 * @param defeated
	 * @param winner
	 * @return Glory Lost in PvP Death
	 */
	public static int calculatePvPGpLost(Player defeated, Player winner) {
        int pointsLost = Math.round(defeated.getAbyssRank().getRank().getPointsLost()
                * defeated.getRates().getGpPlayerLossRate());

        // Level penalty calculation
        int difference = winner.getLevel() - defeated.getLevel();

        if (difference > 4) {
            pointsLost = Math.round(pointsLost * 0.1f);
        } else {
            switch (difference) {
                case 3:
                    pointsLost = Math.round(pointsLost * 0.85f);
                    break;
                case 4:
                    pointsLost = Math.round(pointsLost * 0.65f);
                    break;
            }
        }
        return pointsLost;
    }
	
	/**
	 * @param defeated
	 * @param winner
	 * @return Points Gained in PvP Kill
	 */
	public static int calculatePvpApGained(Player defeated, int maxRank, int maxLevel) {
		int pointsGained = defeated.getAbyssRank().getRank().getPointsGained();
		int difference = maxLevel - defeated.getLevel();
		if (difference > 4) {
			pointsGained = Math.round(pointsGained * 0.1f);
		} else if (difference < -3) {
			pointsGained = Math.round(pointsGained * 1.3f);
		} else {
			switch (difference) {
				case 3:
					pointsGained = Math.round(pointsGained * 0.85f);
				break;
				case 4:
					pointsGained = Math.round(pointsGained * 0.65f);
				break;
				case -2:
					pointsGained = Math.round(pointsGained * 1.1f);
				break;
				case -3:
					pointsGained = Math.round(pointsGained * 1.2f);
				break;
			}
		}
		int winnerAbyssRank = maxRank;
		int defeatedAbyssRank = defeated.getAbyssRank().getRank().getId();
		int abyssRankDifference = winnerAbyssRank - defeatedAbyssRank;
		if (winnerAbyssRank <= 7 && abyssRankDifference > 0) {
			float penaltyPercent = abyssRankDifference * 0.05f;
			pointsGained -= Math.round(pointsGained * penaltyPercent);
		}
		return pointsGained;
	}
	
	/**
	 * @param defeated
	 * @param winner
	 * @return Glory Gained in PvP Kill
	 */
	public static int calculatePvpGpGained(Player defeated, int maxRank, int maxLevel) {
        int pointsGained = defeated.getAbyssRank().getRank().getPointsGained();

        // Level penalty calculation
        int difference = maxLevel - defeated.getLevel();

        if (difference > 4) {
            pointsGained = Math.round(pointsGained * 0.1f);
        } else if (difference < -3) {
            pointsGained = Math.round(pointsGained * 1.3f);
        } else {
            switch (difference) {
                case 3:
                    pointsGained = Math.round(pointsGained * 0.85f);
                    break;
                case 4:
                    pointsGained = Math.round(pointsGained * 0.65f);
                    break;
                case -2:
                    pointsGained = Math.round(pointsGained * 1.1f);
                    break;
                case -3:
                    pointsGained = Math.round(pointsGained * 1.2f);
                    break;
            }
        }

        // Abyss rank penalty calculation
        int winnerAbyssRank = maxRank;
        int defeatedAbyssRank = defeated.getAbyssRank().getRank().getId();
        int abyssRankDifference = winnerAbyssRank - defeatedAbyssRank;

        if (winnerAbyssRank <= 7 && abyssRankDifference > 0) {
            float penaltyPercent = abyssRankDifference * 0.05f;

            pointsGained -= Math.round(pointsGained * penaltyPercent);
        }

        return pointsGained;
    }
	
	public static int calculatePvpXpGained(Player defeated, int maxRank, int maxLevel) {
		int pointsGained = 5000;
		int difference = maxLevel - defeated.getLevel();
		if (difference > 4) {
			pointsGained = Math.round(pointsGained * 0.1f);
		} else if (difference < -3) {
			pointsGained = Math.round(pointsGained * 1.3f);
		} else {
			switch (difference) {
				case 3:
					pointsGained = Math.round(pointsGained * 0.85f);
				break;
				case 4:
					pointsGained = Math.round(pointsGained * 0.65f);
				break;
				case -2:
					pointsGained = Math.round(pointsGained * 1.1f);
				break;
				case -3:
					pointsGained = Math.round(pointsGained * 1.2f);
				break;
			}
		}
		int winnerAbyssRank = maxRank;
		int defeatedAbyssRank = defeated.getAbyssRank().getRank().getId();
		int abyssRankDifference = winnerAbyssRank - defeatedAbyssRank;
		if (winnerAbyssRank <= 7 && abyssRankDifference > 0) {
			float penaltyPercent = abyssRankDifference * 0.05f;
			pointsGained -= Math.round(pointsGained * penaltyPercent);
		}
		return pointsGained;
	}
	
	public static int calculatePvpDpGained(Player defeated, int maxRank, int maxLevel) {
		int pointsGained = 0;
		int baseDp = 1064;
		int dpPerRank = 57;
		pointsGained = (defeated.getAbyssRank().getRank().getId() - maxRank) * dpPerRank + baseDp;  
		pointsGained = StatFunctions.adjustPvpDpGained(pointsGained, defeated.getLevel(), maxLevel);
		return pointsGained;
	}
	
	public static int adjustPvpDpGained(int points, int defeatedLvl, int killerLvl) {
		int pointsGained = points;
		int difference = killerLvl - defeatedLvl;
		if (difference >= 10) {
			pointsGained = 0;
		} else if (difference < 10 && difference >= 0) {
			pointsGained -= pointsGained * difference * 0.1;
		} else if (difference <= -10) {
			pointsGained *= 1.1;
		} else if (difference > -10 && difference < 0) {
			pointsGained += pointsGained * Math.abs(difference) * 0.01;
		}
		return pointsGained;
	}
	
	public static int calculateGroupDPReward(Player player, Creature target) {
		int playerLevel = player.getCommonData().getLevel();
		int targetLevel = target.getLevel();
		NpcRating npcRating = ((Npc) target).getObjectTemplate().getRating();
		int baseDP = targetLevel * calculateRatingMultipler(npcRating);
		int xpPercentage = XPRewardEnum.xpRewardFrom(targetLevel - playerLevel);
		float rate = player.getRates().getDpNpcRate();
		return (int) Math.floor(baseDP * xpPercentage * rate / 100);
	}
	
	/**
	 * Hate based on BOOST_HATE stat Now used only from skills, probably need to use for regular attack
	 * 
	 * @param creature
	 * @param value
	 * @return
	 */
	public static int calculateHate(Creature creature, int value) {
		Stat2 stat = new AdditionStat(StatEnum.BOOST_HATE, value, creature, 0.1f);
		return (int) (creature.getGameStats().getStat(StatEnum.BOOST_HATE, stat).getCurrent());
	}
	
	/**
	 * @param player
	 * @param target
	 * @param isMainHand
	 * @param	element
	 * @return Damage made to target (-hp value)
	 */
	public static int calculateAttackDamage(Creature attacker, Creature target, boolean isMainHand, SkillElement element) {
        int resultDamage = 0;
        if (element == SkillElement.NONE) {
            resultDamage = calculatePhysicalAttackDamage(attacker, target, isMainHand);
        } else {
            resultDamage = calculateMagicalAttackDamage(attacker, target, element, isMainHand);
        }
		elements = element;
        resultDamage = (int) adjustDamages(attacker, target, resultDamage, 0, true);
		if (target instanceof Npc) {
            return target.getAi2().modifyDamage((int) resultDamage);
        } if (attacker instanceof Npc) {
            return attacker.getAi2().modifyOwnerDamage(resultDamage);
        }
        return resultDamage;
    }
	
	/**
	 * @param player
	 * @param target
	 * @param effectTemplate
	 * @param skillDamages
	 * @return Damage made to target (-hp value)
	 */
	public static int calculatePhysicalAttackDamage(Creature attacker, Creature target, boolean isMainHand) {
		Stat2 pAttack;
		if (isMainHand) {
			pAttack = attacker.getGameStats().getMainHandPAttack();
		} else {
			pAttack = ((Player) attacker).getGameStats().getOffHandPAttack();
		}
		float resultDamage = pAttack.getCurrent();
		float baseDamage = pAttack.getBase();
		if (attacker instanceof Player) {
			Equipment equipment = ((Player) attacker).getEquipment();
			Item weapon;
			if (isMainHand) {
				weapon = equipment.getMainHandWeapon();
			} else {
				weapon = equipment.getOffHandWeapon();
			} if (weapon != null) {
				WeaponStats weaponStat = weapon.getItemTemplate().getWeaponStats();
				if (weaponStat == null) {
					return 0;
			    }
				int totalMin = weaponStat.getMinDamage();
				int totalMax = weaponStat.getMaxDamage();
				if (totalMax - totalMin < 1) {
				}
				float power = attacker.getGameStats().getPower().getCurrent() * 0.01f;
				int diff = Math.round((totalMax - totalMin) * power / 2);
				resultDamage = pAttack.getBonus() + baseDamage;
				int negativeDiff = diff;
				if (!isMainHand) {
					negativeDiff = (int)Math.round((200 - ((Player)attacker).getDualEffectValue()) * 0.01 * diff);
				}
				resultDamage += Rnd.get(-negativeDiff, diff);
				if (attacker.isInState(CreatureState.POWERSHARD)) {
					Item firstShard;
					Item secondShard = null;
					if (isMainHand) {
						firstShard = equipment.getMainHandPowerShard();
						if (weapon.getItemTemplate().isTwoHandWeapon()) {
							secondShard = equipment.getOffHandPowerShard();
						}
					} else {
						firstShard = equipment.getOffHandPowerShard();
					} if (firstShard != null) {
						equipment.usePowerShard(firstShard, 1);
						resultDamage += firstShard.getItemTemplate().getWeaponBoost();
					} if (secondShard != null) {
						equipment.usePowerShard(secondShard, 1);
						resultDamage += secondShard.getItemTemplate().getWeaponBoost();
					}
				}
			} else {
				int totalMin = 16;
				int totalMax = 20;
				float power = attacker.getGameStats().getPower().getCurrent() * 0.01f;
				int diff = Math.round((totalMax - totalMin) * power / 2);
				resultDamage = pAttack.getBonus() + baseDamage;
				resultDamage += Rnd.get(-diff, diff);
			}
		} else {
			int rnd = (int) (resultDamage * 0.25);
			resultDamage += Rnd.get(-rnd, rnd);
		}
		float pDef = target.getGameStats().getPDef().getBonus() + getMovementModifier(target, StatEnum.PHYSICAL_DEFENSE, target.getGameStats().getPDef().getBase());
		resultDamage -= (pDef * 0.10f);
		if (resultDamage <= 0) {
			resultDamage = 1;
		}
		return Math.round(resultDamage);
	}
	
	public static int calculateMagicalAttackDamage(Creature attacker, Creature target, SkillElement element, boolean isMainHand) {
        Preconditions.checkNotNull(element, "Skill element should be NONE instead of null");
        Stat2 mAttack;
        if (isMainHand) {
            mAttack = attacker.getGameStats().getMainHandMAttack();
        } else {
            mAttack = attacker.getGameStats().getOffHandMAttack();
        }
        float resultDamage = mAttack.getCurrent();
        if (attacker instanceof Player) {
            Equipment equipment = ((Player) attacker).getEquipment();
            Item weapon = equipment.getMainHandWeapon();
            if (weapon != null) {
                WeaponStats weaponStat = weapon.getItemTemplate().getWeaponStats();
                if (weaponStat == null) {
                    return 0;
                }
                int totalMin = weaponStat.getMinDamage();
                int totalMax = weaponStat.getMaxDamage();
                if (totalMax - totalMin < 1) {
                }
                float knowledge = attacker.getGameStats().getKnowledge().getCurrent() * 0.01f;
                int diff = Math.round((totalMax - totalMin) * knowledge / 2);
                resultDamage = mAttack.getBonus() + getMovementModifier(attacker, StatEnum.MAGICAL_ATTACK, mAttack.getBase());
                resultDamage += Rnd.get(-diff, diff);
                if (attacker.isInState(CreatureState.POWERSHARD)) {
                    Item firstShard = equipment.getMainHandPowerShard();
                    Item secondShard = equipment.getOffHandPowerShard();
                    if (firstShard != null) {
                        equipment.usePowerShard(firstShard, 1);
                        resultDamage += firstShard.getItemTemplate().getWeaponBoost();
                    } if (secondShard != null) {
                        equipment.usePowerShard(secondShard, 1);
                        resultDamage += secondShard.getItemTemplate().getWeaponBoost();
                    }
                }
            }
        } if (element != SkillElement.NONE) {
            float elementalDef = getMovementModifier(target, SkillElement.getResistanceForElement(element), target.getGameStats().getMagicalDefenseFor(element));
            resultDamage = Math.round(resultDamage * (1 - elementalDef / 1300f));
        } if (resultDamage <= 0) {
            resultDamage = 1;
        }
        return Math.round(resultDamage);
    }
	
    public static int calculateMagicalSkillDamage(Creature speller, Creature target, int baseDamages, int bonus, SkillElement element, boolean useMagicBoost, boolean useKnowledge, boolean noReduce, int pvpDamage) {
        CreatureGameStats<?> sgs = speller.getGameStats();
        CreatureGameStats<?> tgs = target.getGameStats();
        int magicBoost = useMagicBoost ? sgs.getMBoost().getCurrent() : 0;
        //int mBResist = tgs.getMBResist().getCurrent();
        //int MDef = tgs.getMDef().getCurrent();
        int knowledge = useKnowledge ? sgs.getKnowledge().getCurrent() : 100;
        if (magicBoost < 0) {
            magicBoost = 0;
        } else if (magicBoost > 3200) {
            magicBoost = 3201;
        }
        float damages = baseDamages * (knowledge / 100f + magicBoost / 1000f);
        damages = sgs.getStat(StatEnum.BOOST_SPELL_ATTACK, (int) damages).getCurrent();
        damages += bonus;
        if (!noReduce && element != SkillElement.NONE) {
            float elementalDef = getMovementModifier(target, SkillElement.getResistanceForElement(element), tgs.getMagicalDefenseFor(element));
            damages = Math.round(damages * (1 - (elementalDef / 1300f)));
        }
		elements = element;
        damages = adjustDamages(speller, target, damages, pvpDamage, useKnowledge);
        if (damages <= 0) {
            damages = 1;
        } if (target instanceof Npc) {
            return target.getAi2().modifyDamage((int) damages);
        }
        return Math.round(damages);
    }
	
	/**
	 * Calculates MAGICAL CRITICAL chance
	 * 
	 * @param attacker
	 * @param attacke
	 * @return boolean
	 */
	public static boolean calculateMagicalCriticalRate(Creature attacker, Creature attacked, int criticalProb) {
		if (attacker instanceof Servant || attacker instanceof Homing) {
		    return false;
		}
		int critical = attacker.getGameStats().getMCritical().getCurrent();
        if (attacked instanceof Player) {
            critical = attacked.getGameStats().getPositiveReverseStat(StatEnum.MAGICAL_CRITICAL_RESIST, critical) + attacked.getGameStats().getPositiveReverseStat(StatEnum.PVP_MAGICAL_RESIST, critical);
        } else {
            critical = attacked.getGameStats().getPositiveReverseStat(StatEnum.MAGICAL_CRITICAL_RESIST, critical);
        }
		critical *= (float) criticalProb / 100f;
		double criticalRate;
		if (critical <= 440) {
			criticalRate = critical * 0.1f;
		} else if (critical <= 600) {
			criticalRate = (440 * 0.1f) + ((critical - 440) * 0.05f);
		} else {
			criticalRate = (440 * 0.1f) + (160 * 0.05f) + ((critical - 600) * 0.02f);
		}
		return Rnd.nextInt(100) < criticalRate;
	}
	
	/**
	 * npcRating
	 */
	public static int calculateRatingMultipler(NpcRating npcRating) {
		int multipler;
		switch (npcRating) {
			case JUNK:
			    multipler = 1;
			break;
			case NORMAL:
				multipler = 2;
			break;
			case ELITE:
				multipler = 3;
			break;
			case HERO:
				multipler = 4;
			break;
			case LEGENDARY:
				multipler = 5;
			break;
			default:
				multipler = 1;
		}
		return multipler;
	}
	
	/**
	 * ApNpcRating
	 */
	public static int ApNpcRating(NpcRating npcRating) {
		int multipler;
		switch (npcRating) {
			case JUNK:
				multipler = 1;
			break;
			case NORMAL:
				multipler = 2;
			break;
			case ELITE:
				multipler = 3;
			break;
			case HERO:
				multipler = 4;
			break;
			case LEGENDARY:
				multipler = 5;
			break;
			default:
				multipler = 1;
		}
		return multipler;
	}
	
	/**
	 * adjust baseDamages according to their level || is PVP?
	 * 
	 * @ref:
	 * @param attacker
	 *          lvl
	 * @param target
	 *          lvl
	 * @param baseDamages
	 **/
	public static float adjustDamages(Creature attacker, Creature target, float damages, int pvpDamage, boolean useMovement) {
        if (attacker instanceof Npc) {
            if (((Npc) attacker).getAi2() != null) {
                if (((Npc) attacker).getAi2().getName().equalsIgnoreCase("artifact")) {
                    return damages;
                }
            }
        } if (attacker.isPvpTarget(target)) {
            if (pvpDamage > 0) {
                damages *= pvpDamage * 0.01;
            }
            damages = Math.round(damages * 0.50f);
            float pvpAttackBonus = attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO, 0).getCurrent();
			float pvpDefenceBonus = target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO, 0).getCurrent();
			float pvpAttackRatioPhyscBonus = attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 0).getCurrent();
			float pvpAttackRatioMagicBonus = target.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 0).getCurrent();
			float pvpDefenceRatioPhyscBonus = attacker.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 0).getCurrent();
			float pvpDefenceRatioMagicBonus = target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 0).getCurrent();
			switch (elements) {
				case NONE:
					pvpAttackBonus += attacker.getGameStats().getStat(StatEnum.PVP_PHYSICAL_ATTACK, 0).getCurrent();
					pvpDefenceBonus += target.getGameStats().getStat(StatEnum.PVP_PHYSICAL_DEFEND, 0).getCurrent();
					pvpAttackRatioPhyscBonus += attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 0).getCurrent();
					pvpAttackRatioMagicBonus += attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 0).getCurrent();
					pvpDefenceRatioPhyscBonus += target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 0).getCurrent();
					pvpDefenceRatioMagicBonus += target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 0).getCurrent();
				break;
				case FIRE:
				case WATER:
				case WIND:
				case EARTH:
				case LIGHT:
				case DARK:
					pvpAttackBonus += attacker.getGameStats().getStat(StatEnum.PVP_MAGICAL_ATTACK, 0).getCurrent();
					pvpDefenceBonus += target.getGameStats().getStat(StatEnum.PVP_MAGICAL_DEFEND, 0).getCurrent();
					pvpAttackRatioPhyscBonus += attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 0).getCurrent();
					pvpAttackRatioMagicBonus += attacker.getGameStats().getStat(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 0).getCurrent();
					pvpDefenceRatioPhyscBonus += target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 0).getCurrent();
					pvpDefenceRatioMagicBonus += target.getGameStats().getStat(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 0).getCurrent();
				break;
				default:
				break;
			}
			pvpAttackBonus = pvpAttackBonus * 0.001f;
			pvpDefenceBonus = pvpDefenceBonus * 0.001f;
			damages = Math.round(damages + (damages * pvpAttackBonus) - (damages * pvpDefenceBonus));
			pvpAttackRatioPhyscBonus = pvpAttackRatioPhyscBonus * 0.001f;
			pvpAttackRatioMagicBonus = pvpAttackRatioMagicBonus * 0.001f;
			pvpDefenceRatioPhyscBonus = pvpDefenceRatioPhyscBonus * 0.001f;
			pvpDefenceRatioMagicBonus = pvpDefenceRatioMagicBonus * 0.001f;
			
			if (attacker.isPhysClass(attacker) && (target.isPhysClass(target) || target.isMagicClass(target))) {
				damages = Math.round(damages + (damages * pvpAttackRatioPhyscBonus) - (damages * pvpDefenceRatioPhyscBonus));
			} else if (attacker.isMagicClass(attacker) && (target.isMagicClass(target) || target.isPhysClass(target))) {
				damages = Math.round(damages + (damages * pvpAttackRatioMagicBonus) - (damages * pvpDefenceRatioMagicBonus));
			} if (attacker.getRace() != target.getRace() && !attacker.isInInstance()) {
                damages *= Influence.getInstance().getPvpRaceBonus(attacker.getRace());
            }
        } else if (target instanceof Npc) {
            int levelDiff = target.getLevel() - attacker.getLevel();
            damages *= (1f - getNpcLevelDiffMod(levelDiff, 0));
        } if (useMovement) {
            damages = movementDamageBonus(attacker, damages);
        } if (attacker instanceof Player) {
        	PlayerClass playerClass = ((Player) attacker).getPlayerClass();
  			if (playerClass != null ) {
  				switch (playerClass) {
  				case GUNSLINGER : damages *= 0.4f;
  					break;
  				case SONGWEAVER : damages *= 0.4f;
  					break;
  				case CLERIC : damages *= 0.4f;
  					break;
  				case SPIRIT_MASTER : damages *= 0.4f;
  					break;
  				case SORCERER : damages *= 0.4f;
  					break;
  				case ASSASSIN : damages *= 0.8f;
  					break;
  				case RANGER : damages *= 0.7f;
  					break;
  				default: damages *= 1f;		
  				}
  			}
        }
        return damages;
    }
	
	/**
	 * Calculates DODGE chance
	 * 
	 * @param attacker
	 * @param attacked
	 * @return boolean
	 */
	public static boolean calculatePhysicalDodgeRate(Creature attacker, Creature attacked, int accMod) {
        if (attacker.getObserveController().checkAttackerStatus(AttackStatus.DODGE)) {
            return true;
        } if (attacked.getObserveController().checkAttackStatus(AttackStatus.DODGE)) {
            return true;
        }
        float accuracy = attacker.getGameStats().getMainHandPAccuracy().getCurrent() + accMod;
        float dodge = 0;
        if (attacked instanceof Player) {
            dodge = attacked.getGameStats().getEvasion().getBonus() + getMovementModifier(attacked, StatEnum.EVASION, attacked.getGameStats().getEvasion().getBase()) + attacked.getGameStats().getStat(StatEnum.PVP_DODGE, 0).getCurrent();
        } else {
            dodge = attacked.getGameStats().getEvasion().getBonus() + getMovementModifier(attacked, StatEnum.EVASION, attacked.getGameStats().getEvasion().getBase());
        }
        float dodgeRate = dodge - accuracy;
        if (attacked instanceof Npc) {
            int levelDiff = attacked.getLevel() - attacker.getLevel();
            dodgeRate *= 1 + getNpcLevelDiffMod(levelDiff, 0);
            if (((Npc) attacked).hasEntity()) {
                return false;
            }
        }
        return calculatePhysicalEvasion(dodgeRate, 300);
    }
	
	/**
	 * Calculates PARRY chance
	 * 
	 * @param attacker
	 * @param attacked
	 * @return int
	 */
	public static boolean calculatePhysicalParryRate(Creature attacker, Creature attacked) {
        if (attacked.getObserveController().checkAttackStatus(AttackStatus.PARRY)) {
            return true;
        }
        float accuracy = attacker.getGameStats().getMainHandPAccuracy().getCurrent();
        float parry = 0;
        if (attacked instanceof Player) {
            parry = attacked.getGameStats().getParry().getBonus() + getMovementModifier(attacked, StatEnum.PARRY, attacked.getGameStats().getParry().getBase()) + attacked.getGameStats().getStat(StatEnum.PVP_PARRY, 0).getCurrent();
        } else {
            parry = attacked.getGameStats().getParry().getBonus() + getMovementModifier(attacked, StatEnum.PARRY, attacked.getGameStats().getParry().getBase());
        }
        float parryRate = parry - accuracy;
        return calculatePhysicalEvasion(parryRate, 400);
    }
	
	/**
	 * Calculates BLOCK chance
	 * 
	 * @param attacker
	 * @param attacked
	 * @return int
	 */
	public static boolean calculatePhysicalBlockRate(Creature attacker, Creature attacked) {
        if (attacked.getObserveController().checkAttackStatus(AttackStatus.BLOCK)) {
            return true;
        }
        float accuracy = attacker.getGameStats().getMainHandPAccuracy().getCurrent();
        float block = 0;
        if (attacked instanceof Player) {
            block = attacked.getGameStats().getBlock().getBonus() + getMovementModifier(attacked, StatEnum.BLOCK, attacked.getGameStats().getBlock().getBase()) + attacked.getGameStats().getStat(StatEnum.PVP_BLOCK, 0).getCurrent();
        } else {
            block = attacked.getGameStats().getBlock().getBonus() + getMovementModifier(attacked, StatEnum.BLOCK, attacked.getGameStats().getBlock().getBase());
        }
        float blockRate = block - accuracy;
        if (blockRate > 500) {
            blockRate = 500;
        }
        return Rnd.nextInt(1000) < blockRate;
    }
	
	/**
	 * Accuracy (includes evasion/parry/block formulas): Accuracy formula is based on opponents evasion/parry/block vs
	 * your own Accuracy. If your Accuracy is 300 or more above opponents evasion/parry/block then you can not be evaded,
	 * parried or blocked. <br>
	 * https://docs.google.com/spreadsheet/ccc?key=0AqxBGNJV9RrzdF9tOWpwUlVLOXE5bVRWeHQtbGQxaUE&hl=en_US#gid=2
	 */
	public static boolean calculatePhysicalEvasion(float diff, int upperCap) {
		diff = diff * 0.6f + 50;
		if (diff > upperCap) {
			diff = upperCap;
		}
		return Rnd.nextInt(1000) < diff;
	}
	
	/**
	 * Calculates CRITICAL chance
	 * http://www.wolframalpha.com/input/?i=quadratic+fit+%7B%7B300%2C+30.97%7D%2C+%7B320%2C+31.68%7D%2C+%7B340%2C+33.30%7D%2C+%7B360%2C+36.09%7D%2C+%7B380%2C+37.81%7D%2C+%7B400%2C+40.72%7D%2C+%7B420%2C+42.12%7D%2C+%7B440%2C+44.03%7D%2C+%7B480%2C+44.66%7D%2C+%7B500%2C+45.96%7D%2C%7B604%2C+51.84%7D%2C+%7B649%2C+52.69%7D%7D
	 * http://www.aionsource.com/topic/40542-character-stats-xp-dp-origin-gerbatorteam-july-2009/
	 * http://www.wolframalpha.com/input/?i=-0.000126341+x%5E2%2B0.184411+x-13.7738
	 * https://docs.google.com/spreadsheet/ccc?key=0AqxBGNJV9RrzdGNjbEhQNHN3S3M5bUVfUVQxRkVIT3c&hl=en_US#gid=0
	 * @param attacker
	 * @return double
	 */
	public static boolean calculatePhysicalCriticalRate(Creature attacker, Creature attacked, boolean isMainHand, int criticalProb, boolean isSkill) {
        if (attacker instanceof Servant || attacker instanceof Homing) {
            return false;
        }
        int critical;
        if (attacker instanceof Player && !isMainHand) {
            critical = ((PlayerGameStats) attacker.getGameStats()).getOffHandPCritical().getCurrent();
        } else {
            critical = attacker.getGameStats().getMainHandPCritical().getCurrent();
        }
        AttackerCriticalStatus acStatus = attacker.getObserveController().checkAttackerCriticalStatus(AttackStatus.CRITICAL, isSkill);
        if (acStatus.isResult()) {
            if (acStatus.isPercent()) {
                critical *= (1 + acStatus.getValue() / 100);
            } else {
                return Rnd.nextInt(1000) < acStatus.getValue();
            }
        }
        critical = attacked.getGameStats().getPositiveReverseStat(StatEnum.PHYSICAL_CRITICAL_RESIST, critical) - attacker.getGameStats().getStat(StatEnum.PVP_HIT_ACCURACY, 0).getCurrent();
        critical *= (float) criticalProb / 100f;
        double criticalRate;
        if (critical <= 500) {
            criticalRate = critical * 0.1f;
        } else if (critical <= 600) {
            criticalRate = (500 * 0.1f) + ((critical - 500) * 0.05f);
        } else {
            criticalRate = (500 * 0.1f) + (160 * 0.05f) + ((critical - 600) * 0.02f);
        }
        return Rnd.nextInt(100) < criticalRate;
    }
	
	/**
	 * Calculates RESIST chance
	 * 
	 * @param attacker
	 * @param attacked
	 * @return int
	 */
	public static int calculateMagicalResistRate(Creature attacker, Creature attacked, int accMod) {
        if (attacked.getObserveController().checkAttackStatus(AttackStatus.RESIST)) {
            return 1000;
        }
        int attackerLevel = attacker.getLevel();
        int targetLevel = attacked.getLevel();
        int resistRate = attacked.getGameStats().getMResist().getCurrent() - attacker.getGameStats().getMAccuracy().getCurrent() - attacker.getGameStats().getStat(StatEnum.PVP_MAGICAL_HIT_ACCURACY, 0).getCurrent() - accMod;
        if ((targetLevel - attackerLevel) > 2) {
            resistRate += (targetLevel - attackerLevel - 2) * 100;
        } if (resistRate <= 0) {
            resistRate = 1;
        } if (resistRate > 500) {
            resistRate = 500;
        }
        return resistRate;
    }
	
	/**
	 * Calculates the fall damage
	 * 
	 * @param player
	 * @param distance
	 * @return True if the player is forced to his bind location.
	 */
	public static boolean calculateFallDamage(Player player, float distance, boolean stoped) {
		if (player.isInvul()) {
			return false;
		} if (distance >= FallDamageConfig.MAXIMUM_DISTANCE_DAMAGE || !stoped) {
			player.getController().onStopMove();
			player.getFlyController().onStopGliding(false);
			player.getLifeStats().reduceHp(player.getLifeStats().getMaxHp() + 1, player);
			return true;
		} else if (distance >= FallDamageConfig.MINIMUM_DISTANCE_DAMAGE) {
			float dmgPerMeter = player.getLifeStats().getMaxHp() * FallDamageConfig.FALL_DAMAGE_PERCENTAGE / 100f;
			int damage = (int) (distance * dmgPerMeter);
			player.getLifeStats().reduceHp(damage, player);
			player.getObserveController().notifyAttackedObservers(player);
			PacketSendUtility.sendPacket(player, new SM_ATTACK_STATUS(player, SM_ATTACK_STATUS.TYPE.FALL_DAMAGE, 0, -damage));
		}
		return false;
	}
	
	public static float getMovementModifier(Creature creature, StatEnum stat, float value) {
        if (!(creature instanceof Player) || stat == null) {
            return value;
		}
        Player player = (Player) creature;
        int h = player.getMoveController().getMovementHeading();
        if (h < 0) {
            return value;
		} switch (h) {
            case 7:
            case 0:
            case 1:
                switch (stat) {
                    case WATER_RESISTANCE:
                    case WIND_RESISTANCE:
                    case FIRE_RESISTANCE:
                    case EARTH_RESISTANCE:
                    case ELEMENTAL_RESISTANCE_DARK:
                    case ELEMENTAL_RESISTANCE_LIGHT:
                    case PHYSICAL_DEFENSE:
                        return value * 0.8f;
				    default:
					break;
                }
            break;
            case 6:
            case 2:
                switch (stat) {
                    case EVASION:
                        return value + 300;
                    case SPEED:
                        return value * 0.8f;
				    default:
					break;
                }
            break;
            case 5:
            case 4:
            case 3:
                switch (stat) {
                    case PARRY:
                    case BLOCK:
                        return value + 500;
                    case SPEED:
                        return value * 0.6f;
				    default:
					break;
                }
            break;
        }
        return value;
    }
	
	private static float movementDamageBonus(Creature creature, float value) {
        if (!(creature instanceof Player)) {
            return value;
		}
        Player player = (Player) creature;
        int h = player.getMoveController().getMovementHeading();
        if (h < 0) {
            return value;
		} switch (h) {
            case 7:
            case 0:
            case 1:
                value = value * 1.1f;
            break;
            case 6:
            case 2:
                value -= value * 0.2f;
            break;
            case 5:
            case 4:
            case 3:
                value -= value * 0.2f;
            break;
        }
        return value;
    }
	
	private static float getNpcLevelDiffMod(int levelDiff, int base) {
        switch (levelDiff) {
            case 3:
                return 0.1f;
            case 4:
                return 0.2f;
            case 5:
                return 0.3f;
            case 6:
                return 0.4f;
            case 7:
                return 0.5f;
            case 8:
                return 0.6f;
            case 9:
                return 0.7f;
            default:
                if (levelDiff > 9)
                return 0.8f;
        }
        return base;
    }
}