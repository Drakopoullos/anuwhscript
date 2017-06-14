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
package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.craft.AetherforgingService;

/**
 * @author Idhacker542
 */

public class CM_AETHERFORGING extends AionClientPacket {
	
	private int action;
	private int targetTemplateId;
	private int recipeId;
	private int targetObjId;
	private int materialsCount;
	private int craftType;

	public CM_AETHERFORGING(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void readImpl() {
		// TODO Auto-generated method stub
		action = readC();
		switch (action) {
			case 0:
				targetTemplateId = readD();
				recipeId = readD();
				targetObjId = readD();
				materialsCount = readH();
				craftType = readC();
				break;
			case 1:
				targetTemplateId = readD();
				recipeId = readD();
				targetObjId = readD();
				materialsCount = readH();
				craftType = readC();
				for (int i = 0; i < materialsCount; i++) {
					readD();
					readQ();
				}
		}
	}

	@Override
	protected void runImpl() {
		// TODO Auto-generated method stub
		Player player = (getConnection().getActivePlayer());
		if(player == null || !player.isSpawned()) {
			return;
		} if (player.getController().isInShutdownProgress()) {
			return;
		} switch (action) {
			case 0:
				AetherforgingService.sendCencelAetherforging(player);
				break;
			case 1:
				AetherforgingService.startAetherforging(player, recipeId, craftType);
		}
	}
	
}