package com.aionemu.gameserver.model.templates.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Component")
public class Component
{
	@XmlElement(name = "component")
	protected ArrayList<ComponentElement> component;
	@XmlAttribute
	protected int itemid;
	@XmlAttribute
	protected int quantity;
	
	public Collection<ComponentElement> getComponents() {
		return component != null ? component : Collections.<ComponentElement> emptyList();
	}

	public Integer getQuantity() {
		return itemid;
	}

	public Integer getItemid() {
		return itemid;
	}
}