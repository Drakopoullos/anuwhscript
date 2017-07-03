package com.aionemu.gameserver.network.aion.clientpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EVENT_DICE;
import com.aionemu.gameserver.utils.PacketSendUtility;

public class CM_EVENT_DICE extends AionClientPacket {

	private int type;
	private int dice;
	private static final Logger log = LoggerFactory.getLogger(CM_EVENT_DICE.class);

	public CM_EVENT_DICE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		type = readC();
		switch (type) {
		case 0:
			break;
		case 1:
			dice = readC();
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		} if (type == 1) {
			PacketSendUtility.sendPacket(player, new SM_EVENT_DICE(0));
		} else {}
	}
}
