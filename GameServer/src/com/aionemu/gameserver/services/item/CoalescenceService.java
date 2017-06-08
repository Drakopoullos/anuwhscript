/*
 * This file is part of Encom. **ENCOM FUCK OTHER SVN**
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.services.item;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_COALESCENCE_RESULT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;

import javolution.util.FastMap;

/**
 * @author Ranastic
 */

public class CoalescenceService
{
	private Logger log = LoggerFactory.getLogger(CoalescenceService.class);
	
	public void letsCoalescence(final Player player, int mainItemObjId, int materialCount, final List<Integer> materialItemObjId) {
		final Item main = player.getInventory().getItemByObjId(mainItemObjId);
		if (main.getEnchantLevel() == 25) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_ENCHANT_ITEM);
			return;
		}
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), main.getObjectId(), main.getItemId(), 4000, 23, 68), true);
		final ItemUseObserver observer = new ItemUseObserver() {
            @Override
            public void abort() {
                player.getController().cancelTask(TaskId.ITEM_USE);
                player.removeItemCoolDown(main.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(main.getItemTemplate().getNameId())));
                PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), main.getObjectId(), main.getItemId(), 0, 2, 0), true);
                player.getObserveController().removeObserver(this);
            }
        };
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, Integer> a = new FastMap<Integer, Integer>();
				player.getObserveController().removeObserver(observer);
				for (int i=0;i<materialItemObjId.size();i++) {
					final Item mats = player.getInventory().getItemByObjId(materialItemObjId.get(i));
					a.put(mats.getItemId(), mats.getEnchantLevel());
					player.getInventory().delete(mats, ItemDeleteType.COALESCENCE);
				}
				int maxLvl = 0;
				int minLvl = 0;
				int minValueInMap = Collections.min(a.values());
				int maxValueInMap = Collections.max(a.values());
				minLvl = minValueInMap;
				if (minLvl == 0) {
					minLvl = 1;
				}
				maxLvl = (maxValueInMap + materialItemObjId.size());
				if (maxLvl > 25) {
					maxLvl = 25;
				} if (minLvl > maxLvl) {
					minLvl = maxLvl;
				}
				int enchantLvl = Rnd.get(minLvl, maxLvl);
				main.setEnchantLevel(enchantLvl);
				ItemPacketService.updateItemAfterInfoChange(player, main, ItemUpdateType.STATS_CHANGE);
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), main.getObjectId(), main.getItemId(), 0, 24, 0), true);
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403620, new DescriptionId(main.getItemTemplate().getNameId())));
				PacketSendUtility.sendPacket(player, new SM_COALESCENCE_RESULT(main.getItemId(), main.getObjectId()));
			}
		}, 4000));
	}
	
	public static CoalescenceService getInstance() {
		return NewSingletonHolder.INSTANCE;
	}
	
	private static class NewSingletonHolder {
		private static final CoalescenceService INSTANCE = new CoalescenceService();
	}
}