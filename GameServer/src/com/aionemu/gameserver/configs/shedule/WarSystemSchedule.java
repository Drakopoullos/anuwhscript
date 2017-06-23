package com.aionemu.gameserver.configs.shedule;

import java.io.File;
import java.util.List;

import javax.xml.bind.annotation.*;

import org.apache.commons.io.FileUtils;

import com.aionemu.commons.utils.xml.JAXBUtil;

/**
 * 
 * @author Idhacker542
 *
 */
@XmlRootElement(name= "warsystem_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class WarSystemSchedule {
	@XmlElement(name = "war", required = true)
	private List<War> warsList;
	
	public List<War> getWarsList() {
		return warsList;
	}
	
	public void setWarsList(List<War> warList) {
		this.warsList = warList;
	}
	
	public static WarSystemSchedule load() {
		WarSystemSchedule ws;
		try {
			String xml = FileUtils.readFileToString(new File("./config/shedule/war_schedule.xml"));
			ws = (WarSystemSchedule) JAXBUtil.deserialize(xml, WarSystemSchedule.class);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize WarSystem", ex);
		}
		return ws;
	}
	
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "war")
	public static class War {
		@XmlAttribute(required = true)
		private int id;
		
		@XmlElement(name = "warTime", required = true)
		private List<String> warTimes;
		
		public int getId() {
			return id;
		}
		
		public void setId(int id) {
			this.id = id;
		}
		
		public List<String> getWarTimes() {
			return warTimes;
		}
		
		public void setWarTimes(List<String> warTimes) {
			this.warTimes = warTimes;
		}
	}
}
