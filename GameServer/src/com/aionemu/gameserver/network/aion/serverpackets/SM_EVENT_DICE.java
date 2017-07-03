package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

public class SM_EVENT_DICE  extends AionServerPacket {

	private int login;
	private int trys;
	private int roll = Rnd.get(1, 6);
	private int test;
	private Player player;
	
	public SM_EVENT_DICE(int login) {
		this.login = login;
	}
	
	public SM_EVENT_DICE(int login, int trys) {
		this.login = login;
		this.trys = trys;
	}
	
	@Override
    protected void writeImpl(AionConnection con) {
		switch (login) {
			case 0:
				writeD(0x02);
				writeD(0x00 + roll);
				writeD(0x0F);
				writeD(0x00);
				writeD(0x03);
				writeD(0x06);
				writeD(0x01);
				writeD(0x91075);
				writeD(0x00);
				writeD(0x91075);
				writeD(0x00);
				writeD(roll);
			case 1:
				writeD(0x02);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x03);
				writeD(0x06);
				writeD(0x01);
				writeD(0x91075);
				writeD(0x00);
				writeD(0x91075);
				writeD(0x00);
				writeD(0x00);
		}	
	}

}
