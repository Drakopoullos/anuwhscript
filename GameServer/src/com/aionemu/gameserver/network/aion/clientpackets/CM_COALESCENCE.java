package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.item.CoalescenceService;

/**
 * @author Ranastic
 */
public class CM_COALESCENCE extends AionClientPacket
{
	private Logger log = LoggerFactory.getLogger(CM_COALESCENCE.class);
	private int mainItemObjId;
	private int materialCount;
	private List<Integer> materialItemObjId;
	
	public CM_COALESCENCE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	
	@Override
	protected void readImpl() {
		materialItemObjId  = new ArrayList<Integer>();
		mainItemObjId = readD();
		materialCount = readH();
		for (int i=0;i<materialCount;i++) {
			materialItemObjId.add(readD());
		}
	}
	
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null || !player.isSpawned()) {
			return;
		} 
		if (player.getController().isInShutdownProgress()) {
			return;
		}
		CoalescenceService.getInstance().letsCoalescence(player, mainItemObjId, materialCount, materialItemObjId);
	}
}
