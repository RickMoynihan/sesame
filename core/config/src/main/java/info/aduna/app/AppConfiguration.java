/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package info.aduna.app;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import info.aduna.app.config.Configuration;
import info.aduna.app.logging.LogConfiguration;
import info.aduna.app.net.ProxySettings;
import info.aduna.app.util.ConfigurationUtil;
import info.aduna.io.MavenUtil;
import info.aduna.platform.PlatformFactory;

import org.openrdf.Sesame;

/**
 * @author Herko ter Horst
 */
public class AppConfiguration implements Configuration {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String APP_CONFIG_FILE = "application.properties";

	private static final String DEFAULT_PREFIX = "Aduna";

	private static final String DEFAULT_LOGGING = "info.aduna.app.logging.logback.LogbackConfiguration";

	/*-----------*
	 * Variables *
	 *-----------*/

	private String applicationId;

	private String longName;

	private String fullName;

	private AppVersion version;

	private String[] commandLineArgs;

	private String dataDirName;

	private File dataDir;

	private LogConfiguration loggingConfiguration;

	private ProxySettings proxySettings;

	private Properties properties;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Create a new, uninitialized application configuration.
	 */
	public AppConfiguration() {
		super();
	}

	/**
	 * Create the application configuration.
	 * 
	 * @param applicationId
	 *        the ID of the application
	 */
	public AppConfiguration(final String applicationId) {
		this();
		setApplicationId(applicationId);
	}

	/**
	 * Create the application configuration.
	 * 
	 * @param applicationId
	 *        the ID of the application
	 * @param version
	 *        the application's version
	 */
	public AppConfiguration(final String applicationId, final AppVersion version) {
		this(applicationId);
		setVersion(version);
	}

	/**
	 * Create the application configuration.
	 * 
	 * @param applicationId
	 *        the ID of the application
	 * @param longName
	 *        the long name of the application
	 */
	public AppConfiguration(final String applicationId, final String longName) {
		this(applicationId);
		setLongName(longName);
	}
	
	/**
	 * Create the application configuration.
	 * 
	 * @param applicationId
	 *        the ID of the application
	 * @param longName
	 *        the long name of the application
	 * @param version
	 *        the application's version
	 */
	public AppConfiguration(final String applicationId, final String longName, final AppVersion version) {
		this(applicationId, version);
		setLongName(longName);
	}

	/*---------*
	 * Methods *
	 ----------*/

	public void load()
		throws IOException
	{
		properties = ConfigurationUtil.loadConfigurationProperties(APP_CONFIG_FILE, null);
	}

	public void save()
		throws IOException
	{
		if (null != loggingConfiguration) {
			loggingConfiguration.save();
		}
		proxySettings.save();
	}

	public void init()
		throws IOException
	{
		this.init(true);
	}

	public void init(final boolean loadLogConfig)
		throws IOException
	{
		if (longName == null) {
			setLongName(DEFAULT_PREFIX + " " + applicationId);
		}
		setFullName();
		configureDataDir();
		load();
		if (loadLogConfig) {
			try {
				loggingConfiguration = loadLogConfiguration();
				loggingConfiguration.setBaseDir(getDataDir());
				loggingConfiguration.setAppConfiguration(this);
				loggingConfiguration.init();
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			catch (InstantiationException e) {
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		proxySettings = new ProxySettings(getDataDir());
		proxySettings.init();
		save();
	}

	public void destroy()
		throws IOException
	{
		loggingConfiguration.destroy();
		// proxySettings.destroy();
	}

	/**
	 * Get the name of the application (e.g. "AutoFocus" or "Metadata Server").
	 * 
	 * @return the name of the application
	 */
	public String getApplicationId() {
		return applicationId;
	}

	public final void setApplicationId(final String applicationId) {
		this.applicationId = applicationId;
	}

	public void setDataDirName(final String dataDirName) {
		this.dataDirName = dataDirName;
	}

	/**
	 * Get the long name of the application (e.g. "Aduna AutoFocus" or "OpenRDF
	 * Sesame Server").
	 * 
	 * @return the long name of the application
	 */
	public String getLongName() {
		return longName;
	}

	/**
	 * Set the long name of the application.
	 * 
	 * @param longName
	 *        the new name
	 */
	public final void setLongName(final String longName) {
		this.longName = longName;
	}

	/**
	 * Get the full name of the application, which consists of the long name and
	 * the version number (e.g. "Aduna AutoFocus 4.0-beta1" or "OpenRDF Sesame
	 * Webclient 2.0")
	 * 
	 * @return the full name of the application
	 */
	public String getFullName() {
		return fullName;
	}

	private void setFullName() {
		this.fullName = longName;
		if (version != null) {
			fullName = fullName + " " + version.toString();
		}
	}

	/**
	 * Get the version of the application.
	 * 
	 * @return the version of the application
	 */
	public AppVersion getVersion() {
		if (version == null) {
			version = AppVersion.parse(Sesame.getVersion());
		}
		return version;
	}

	/**
	 * Set the version of the application.
	 * 
	 * @param version
	 *        the new version
	 */
	public final void setVersion(final AppVersion version) {
		this.version = version;
		this.fullName = longName + " " + version.toString();
	}

	/**
	 * Get the command line arguments of the application.
	 * 
	 * @return A String array, as (typically) specified to the main method.
	 */
	public String[] getCommandLineArgs() {
		return (String[])commandLineArgs.clone();
	}

	/**
	 * Set the command line arguments specified to the application.
	 * 
	 * @param args
	 *        A String array containing the arguments as specified to the main
	 *        method.
	 */
	public void setCommandLineArgs(final String[] args) {
		this.commandLineArgs = (String[])args.clone();
	}

	public File getDataDir() {
		return dataDir;
	}

	public LogConfiguration getLogConfiguration() {
		return loggingConfiguration;
	}

	public ProxySettings getProxySettings() {
		return proxySettings;
	}

	public void setProxySettings(final ProxySettings proxySettings) {
		this.proxySettings = proxySettings;
	}

	/**
	 * Configure the data dir.
	 * 
	 * @param dataDirParam
	 *        the data dir to use. If null, determination of the data dir will be
	 *        deferred to Platform.
	 */
	private void configureDataDir() {
		if (dataDirName != null) {
			dataDirName = dataDirName.trim();
			if (!("".equals(dataDirName))) {
				final File dataDirCandidate = new File(dataDirName);
				dataDirCandidate.mkdirs();
				// change data directory if the previous code was successful
				dataDir = (dataDirCandidate.canRead() && dataDirCandidate.canWrite()) ? dataDirCandidate
						: dataDir;
			}
		}
		if (dataDir == null) {
			dataDir = PlatformFactory.getPlatform().getApplicationDataDir(applicationId);
		}
	}

	/**
	 * Load and instantiate the logging configuration.
	 * 
	 * @return the logging configuration
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private LogConfiguration loadLogConfiguration()
		throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		String classname = this.properties.getProperty("feature.logging.impl");
		if (classname == null) {
			classname = DEFAULT_LOGGING;
		}
		final Class<?> logImplClass = Class.forName(classname);
		final Object logImpl = logImplClass.newInstance();
		if (logImpl instanceof LogConfiguration) {
			return (LogConfiguration)logImpl;
		}
		throw new InstantiationException(classname + " is not valid LogConfiguration instance!");
	}

	/**
	 * @return Returns the properties.
	 */
	public Properties getProperties() {
		return properties;
	}
}
