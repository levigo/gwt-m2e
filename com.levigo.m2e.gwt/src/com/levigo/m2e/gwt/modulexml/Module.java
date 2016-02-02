package com.levigo.m2e.gwt.modulexml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "module")
public class Module {
	@XmlElement(name="source")
	public List<Source> sources;
	
	@XmlElement(name="super-source")
	public List<Source> superSources;
}
