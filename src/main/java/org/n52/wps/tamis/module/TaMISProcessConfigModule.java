package org.n52.wps.tamis.module;

import java.util.Arrays;
import java.util.List;

import org.n52.wps.tamis.DummyAlgorithmRepository;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationKey;
import org.n52.wps.webapp.api.FormatEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;

public class TaMISProcessConfigModule extends ClassKnowingModule {

	private boolean active = true;

	public static final String talsimTaskManagerPathKey = "talsimTaskManager_path";

	public static final String talsimTALSIMtoFEWSDataPathKey = "talsimTALSIMtoFEWSData_path";
	
	public static final String talsimFEWStoTALSIMDataPathKey = "talsimFEWStoTALSIMData_path";
	
	private ConfigurationEntry<String> talsimTaskManagerPathEntry = new StringConfigurationEntry(talsimTaskManagerPathKey, "TalSIM TaskManager path", "Path to TalSIM TaskManager executable",
			true, "D:/Programme/talsim-ng/customers/wv/applications/TaskMgr/SydroTaskMgr.exe");
	
	private ConfigurationEntry<String> talsimFEWStoTALSIMDataPathEntry = new StringConfigurationEntry(talsimFEWStoTALSIMDataPathKey, "TalSIM FEWStoTALSIM path", "Path to TalSIM input data directory",
			true, "D:/Programme/talsim-ng/customers/wv/projectData/tamis/extern/FEWStoTALSIM");
	
	private ConfigurationEntry<String> talsimTALSIMtoFEWSDataPathEntry = new StringConfigurationEntry(talsimTALSIMtoFEWSDataPathKey, "TalSIM TALSIMtoFEWS path", "Path to TalSIM output data directory",
			true, "D:/Programme/talsim-ng/customers/wv/projectData/tamis/extern/TALSIMtoFEWS");

	private List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(talsimTaskManagerPathEntry, talsimFEWStoTALSIMDataPathEntry, talsimTALSIMtoFEWSDataPathEntry);
	
	private String talsimTALSIMtoFEWSDataPath;	

	private String talsimFEWStoTALSIMDataPath;	
	
	private String talsimTaskManagerPath;
	
	@Override
	public String getModuleName() {
		return "TaMIS processes config module.";
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public ConfigurationCategory getCategory() {
		return ConfigurationCategory.REPOSITORY;
	}

	@Override
	public List<? extends ConfigurationEntry<?>> getConfigurationEntries() {
		return configurationEntries;
	}

	@Override
	public List<AlgorithmEntry> getAlgorithmEntries() {
		return null;//TODO, can we show the algorithms here somehow?
	}

	@Override
	public List<FormatEntry> getFormatEntries() {
		return null;
	}

	public String getTalSIMTaskManagerPath() {
		return talsimTaskManagerPath;
	}

	@ConfigurationKey(key = talsimTaskManagerPathKey)
	public void setTalSIMTaskManagerPath(String talsimTaskManagerPath) {
		this.talsimTaskManagerPath = talsimTaskManagerPath;
	}

	public String getTalsimTALSIMtoFEWSDataPath() {
		return talsimTALSIMtoFEWSDataPath;
	}

	@ConfigurationKey(key = talsimTALSIMtoFEWSDataPathKey)
	public void setTalsimTALSIMtoFEWSDataPath(String talsimTALSIMtoFEWSDataPath) {
		this.talsimTALSIMtoFEWSDataPath = talsimTALSIMtoFEWSDataPath;
	}

	public String getTalsimFEWStoTALSIMDataPath() {
		return talsimFEWStoTALSIMDataPath;
	}

	@ConfigurationKey(key = talsimFEWStoTALSIMDataPathKey)
	public void setTalsimFEWStoTALSIMDataPath(String talsimFEWStoTALSIMDataPath) {
		this.talsimFEWStoTALSIMDataPath = talsimFEWStoTALSIMDataPath;
	}

	@Override
	public String getClassName() {
		return DummyAlgorithmRepository.class.getName();
	}

}
