/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher. If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import com.beust.jcommander.JCommander;

import net.technicpack.autoupdate.IBuildNumber;
import net.technicpack.autoupdate.Relauncher;
import net.technicpack.discord.CacheDiscordApi;
import net.technicpack.discord.HttpDiscordApi;
import net.technicpack.discord.IDiscordApi;
import net.technicpack.launcher.io.TechnicAvatarMapper;
import net.technicpack.launcher.io.TechnicFaceMapper;
import net.technicpack.launcher.io.TechnicInstalledPackStore;
import net.technicpack.launcher.io.TechnicLauncherDirectories;
import net.technicpack.launcher.io.TechnicUserStore;
import net.technicpack.launcher.launch.Installer;
import net.technicpack.launcher.settings.SettingsFactory;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.settings.migration.IMigrator;
import net.technicpack.launcher.settings.migration.InitialV3Migrator;
import net.technicpack.launcher.ui.InstallerFrame;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.LoginFrame;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserStore;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.image.face.CrafatarFaceImageStore;
import net.technicpack.launchercore.image.face.WebAvatarImageStore;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.source.FileJavaSource;
import net.technicpack.launchercore.launch.java.source.InstalledJavaSource;
import net.technicpack.launchercore.logging.BuildLogFormatter;
import net.technicpack.launchercore.logging.RotatingFileHandler;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.mirror.secure.rest.JsonWebSecureMirror;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.PackLoader;
import net.technicpack.launchercore.modpacks.resources.PackImageStore;
import net.technicpack.launchercore.modpacks.resources.PackResourceMapper;
import net.technicpack.launchercore.modpacks.resources.resourcetype.BackgroundResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IModpackResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IconResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.LogoResourceType;
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.minecraftcore.launch.MinecraftLauncher;
import net.technicpack.minecraftcore.mojang.auth.AuthenticationService;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.IPlatformSearchApi;
import net.technicpack.platform.PlatformPackInfoRepository;
import net.technicpack.platform.cache.ModpackCachePlatformApi;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.platform.http.HttpPlatformSearchApi;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.SolderPackSource;
import net.technicpack.solder.cache.CachedSolderApi;
import net.technicpack.solder.http.HttpSolderApi;
import net.technicpack.ui.components.Console;
import net.technicpack.ui.components.ConsoleFrame;
import net.technicpack.ui.components.ConsoleHandler;
import net.technicpack.ui.components.LoggerOutputStream;
import net.technicpack.ui.controls.installation.SplashScreen;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;

public class LauncherMain {

	public static ConsoleFrame consoleFrame;

	public static Locale[] supportedLanguages = new Locale[] {
			Locale.ENGLISH,
			new Locale("pt", "BR"),
			new Locale("pt", "PT"),
			new Locale("cs"),
			Locale.GERMAN,
			Locale.FRENCH,
			Locale.ITALIAN,
			new Locale("hu"),
			new Locale("pl"),
			Locale.CHINA,
			Locale.TAIWAN
	};

	public static final String SOLDER_API = "http://sd.kmecpp.com/api/";
	public static SplashScreen splash;

	public static void main(String[] args) throws IOException {
		//		File file = new File("vcm-log.txt");
		//		if (!file.exists()) {
		//			file.createNewFile();
		//		}
		//		System.setOut(new PrintStream(new FileOutputStream(file), true));
		//		System.setErr(new PrintStream(new FileOutputStream("vcm-log.txt"), true));
		//		Runtime.getRuntime().exec(new String[] { "java", "-jar", "\"" + LauncherMain.class.get + "\"" });

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
		}

		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		StartupParameters params = new StartupParameters(args);
		try {
			new JCommander(params, args);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		TechnicSettings settings = null;

		try {
			settings = SettingsFactory.buildSettingsObject(Relauncher.getRunningPath(LauncherMain.class), params.isMover());
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}

		if (settings == null) {
			ResourceLoader installerResources = new ResourceLoader(null, "net", "technicpack", "launcher", "resources");
			installerResources.setSupportedLanguages(supportedLanguages);
			installerResources.setLocale(ResourceLoader.DEFAULT_LOCALE);
			InstallerFrame dialog = new InstallerFrame(installerResources, params);
			dialog.setVisible(true);
			return;
		}

		LauncherDirectories directories = new TechnicLauncherDirectories(settings.getTechnicRoot());
		ResourceLoader resources = new ResourceLoader(directories, "net", "technicpack", "launcher", "resources");
		resources.setSupportedLanguages(supportedLanguages);
		resources.setLocale(settings.getLanguageCode());

		final int build = 1000;
		IBuildNumber buildNumber = new IBuildNumber() {

			@Override
			public String getBuildNumber() {
				return String.valueOf(build);
			}
		};

		setupLogging(directories, resources, buildNumber);

		//		Relauncher launcher = new TechnicRelauncher(new HttpUpdateStream("http://api.technicpack.net/launcher/"), settings.getBuildStream() + "4", build, directories, resources, params);

		try {
			//			if (launcher.runAutoUpdater()) {
			startLauncher(settings, params, directories, resources, buildNumber);
			//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void setupLogging(LauncherDirectories directories, ResourceLoader resources, IBuildNumber buildNumber) {
		System.out.println("Setting up logging");
		final Logger logger = Utils.getLogger();
		File logDirectory = new File(directories.getLauncherDirectory(), "logs");
		if (!logDirectory.exists()) {
			logDirectory.mkdir();
		}
		File logs = new File(logDirectory, "techniclauncher_%D.log");
		RotatingFileHandler fileHandler = new RotatingFileHandler(logs.getPath());

		fileHandler.setFormatter(new BuildLogFormatter(buildNumber.getBuildNumber()));

		for (Handler h : logger.getHandlers()) {
			logger.removeHandler(h);
		}
		logger.addHandler(fileHandler);
		logger.setUseParentHandlers(false);

		LauncherMain.consoleFrame = new ConsoleFrame(2500, resources.getImage("icon.png"));
		Console console = new Console(LauncherMain.consoleFrame, buildNumber.getBuildNumber());

		logger.addHandler(new ConsoleHandler(console));

		System.setOut(new PrintStream(new LoggerOutputStream(console, Level.INFO, logger), true));
		System.setErr(new PrintStream(new LoggerOutputStream(console, Level.SEVERE, logger), true));

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
				logger.log(Level.SEVERE, "Unhandled Exception in " + t, e);

				//                if (errorDialog == null) {
				//                    LauncherFrame frame = null;
				//
				//                    try {
				//                        frame = Launcher.getFrame();
				//                    } catch (Exception ex) {
				//                        //This can happen if we have a very early crash- before Launcher initializes
				//                    }
				//
				//                    errorDialog = new ErrorDialog(frame, e);
				//                    errorDialog.setVisible(true);
				//                }
			}
		});
	}

	private static void startLauncher(final TechnicSettings settings, StartupParameters startupParameters, final LauncherDirectories directories, ResourceLoader resources, IBuildNumber buildNumber) {
		UIManager.put("ComboBox.disabledBackground", LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
		UIManager.put("ComboBox.disabledForeground", LauncherFrame.COLOR_GREY_TEXT);
		System.setProperty("xr.load.xml-reader", "org.ccil.cowan.tagsoup.Parser");

		//Remove all log files older than a week
		new Thread(new Runnable() {

			@Override
			public void run() {
				Iterator<File> files = FileUtils.iterateFiles(new File(directories.getLauncherDirectory(), "logs"), new String[] { "log" }, false);
				while (files.hasNext()) {
					File logFile = files.next();
					if (logFile.exists() && (new DateTime(logFile.lastModified())).isBefore(DateTime.now().minusWeeks(1))) {
						logFile.delete();
					}
				}
			}
		}).start();

		Utils.getLogger().info("OS: " + System.getProperty("os.name").toLowerCase(Locale.ENGLISH));
		Utils.getLogger().info("Identified as " + OperatingSystem.getOperatingSystem().getName());

		splash = new SplashScreen(resources.getImage("logo.png"), 0);
		Color bg = LauncherFrame.COLOR_FORMELEMENT_INTERNAL;
		splash.getContentPane().setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 255));
		splash.pack();
		splash.setLocationRelativeTo(null);
		splash.setVisible(true);

		boolean loadedAether = false;

		try {
			if (Class.forName("org.apache.maven.repository.internal.MavenRepositorySystemUtils", false, ClassLoader.getSystemClassLoader()) != null) {
				loadedAether = true;
			}
		} catch (ClassNotFoundException ex) {
			//Aether is not loaded
		}
		System.out.println("DONE");
		splash.dispose();

		if (!loadedAether) {
			File launcherAssets = new File(directories.getAssetsDirectory(), "launcher");

			File aether = new File(launcherAssets, "aether-dep.jar");

			try {
				ClassLoader loader = ClassLoader.getSystemClassLoader();
				if (loader instanceof URLClassLoader) {
					Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
					m.setAccessible(true);
					m.invoke(loader, aether.toURI().toURL());
				} else {
					loader = new URLClassLoader(new URL[] { aether.toURI().toURL() }, ClassLoader.getSystemClassLoader());
					Field scl = ClassLoader.class.getDeclaredField("scl"); // Get system class loader
					scl.setAccessible(true); // Set accessible
					scl.set(null, loader);
					Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
					m.setAccessible(true);
					m.invoke(loader, aether.toURI().toURL());
				}
				//				 else {
				//					URLClassLoader l = new URLClassLoader(new URL[] { aether.toURI().toURL() }, ClassLoader.getSystemClassLoader());
				//					JarFile jarFile = new JarFile(aether.getAbsoluteFile());
				//					Enumeration<JarEntry> e = jarFile.entries();
				//
				//					while (e.hasMoreElements()) {
				//						JarEntry je = e.nextElement();
				//						if (je.isDirectory() || !je.getName().endsWith(".class")) {
				//							continue;
				//						}
				//						String className = je.getName().substring(0, je.getName().length() - 6);
				//						className = className.replace('/', '.');
				//						l.loadClass(className);
				//						System.out.println("Loaded: " + className);
				//
				//					}
				//					jarFile.close();
				//					l.close();
				//					//					l.loadClass("org.eclipse.aether.impl.DefaultServiceLocator");
				//					//					l.loadClass("org.eclipse.aether.impl.DefaultServiceLocator$ErrorHandler");
				//					//					l.loadClass("org.eclipse.aether.collection.DependencyCollectionException");
				//					//
				//					//					l.close();
				//				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		JavaVersionRepository javaVersions = new JavaVersionRepository();
		(new InstalledJavaSource()).enumerateVersions(javaVersions);
		FileJavaSource javaVersionFile = FileJavaSource.load(new File(settings.getTechnicRoot(), "javaVersions.json"));
		javaVersionFile.enumerateVersions(javaVersions);
		javaVersions.selectVersion(settings.getJavaVersion(), settings.getJavaBitness());

		IUserStore<MojangUser> users = TechnicUserStore.load(new File(directories.getLauncherDirectory(), "users.json"));
		UserModel userModel = new UserModel(users, new AuthenticationService());
		System.out.println("DONE");

		MirrorStore mirrorStore = new MirrorStore(userModel);
		mirrorStore.addSecureMirror("mirror.technicpack.net", new JsonWebSecureMirror("http://mirror.technicpack.net/", "mirror.technicpack.net"));

		IModpackResourceType iconType = new IconResourceType();
		IModpackResourceType logoType = new LogoResourceType();
		IModpackResourceType backgroundType = new BackgroundResourceType();

		PackResourceMapper iconMapper = new PackResourceMapper(directories, resources.getImage("icon.png"), iconType);
		ImageRepository<ModpackModel> iconRepo = new ImageRepository<ModpackModel>(iconMapper, new PackImageStore(iconType, mirrorStore, userModel));
		ImageRepository<ModpackModel> logoRepo = new ImageRepository<ModpackModel>(new PackResourceMapper(directories, resources.getImage("modpack/ModImageFiller.png"), logoType),
				new PackImageStore(logoType, mirrorStore, userModel));
		ImageRepository<ModpackModel> backgroundRepo =
				new ImageRepository<ModpackModel>(new PackResourceMapper(directories, null, backgroundType), new PackImageStore(backgroundType, mirrorStore, userModel));

		ImageRepository<IUserType> skinRepo = new ImageRepository<IUserType>(new TechnicFaceMapper(directories, resources), new CrafatarFaceImageStore("http://crafatar.com/", mirrorStore));

		ImageRepository<AuthorshipInfo> avatarRepo = new ImageRepository<AuthorshipInfo>(new TechnicAvatarMapper(directories, resources), new WebAvatarImageStore(mirrorStore));

		HttpSolderApi httpSolder = new HttpSolderApi(settings.getClientId(), userModel);
		ISolderApi solder = new CachedSolderApi(directories, httpSolder, 60 * 60);
		HttpPlatformApi httpPlatform = new HttpPlatformApi("http://api.technicpack.net/", mirrorStore, buildNumber.getBuildNumber());

		IPlatformApi platform = new ModpackCachePlatformApi(httpPlatform, 60 * 60, directories);
		IPlatformSearchApi platformSearch = new HttpPlatformSearchApi("http://api.technicpack.net/", buildNumber.getBuildNumber());

		IInstalledPackRepository packStore = TechnicInstalledPackStore.load(new File(directories.getLauncherDirectory(), "installedPacks"));
		IAuthoritativePackSource packInfoRepository = new PlatformPackInfoRepository(platform, solder);

		ArrayList<IMigrator> migrators = new ArrayList<IMigrator>(1);
		migrators.add(new InitialV3Migrator(platform));
		SettingsFactory.migrateSettings(settings, packStore, directories, users, migrators);

		PackLoader packList = new PackLoader(directories, packStore, packInfoRepository);
		ModpackSelector selector = new ModpackSelector(resources, packList, new SolderPackSource("http://solder.technicpack.net/api/", solder), solder, platform, platformSearch, iconRepo);
		selector.setBorder(BorderFactory.createEmptyBorder());
		userModel.addAuthListener(selector);

		resources.registerResource(selector);

		DiscoverInfoPanel discoverInfoPanel = new DiscoverInfoPanel(resources, startupParameters.getDiscoverUrl(), platform, directories, selector);

		MinecraftLauncher launcher = new MinecraftLauncher(platform, directories, userModel, javaVersions);
		ModpackInstaller modpackInstaller = new ModpackInstaller(platform, settings.getClientId());
		Installer installer = new Installer(startupParameters, mirrorStore, directories, modpackInstaller, launcher, settings, iconMapper);

		IDiscordApi discordApi = new HttpDiscordApi("https://discordapp.com/api/");
		discordApi = new CacheDiscordApi(discordApi, 600, 60);

		final LauncherFrame frame = new LauncherFrame(resources, skinRepo, userModel, settings, selector, iconRepo, logoRepo, backgroundRepo, installer, avatarRepo, platform, directories, packStore,
				startupParameters, discoverInfoPanel, javaVersions, javaVersionFile, buildNumber, discordApi);
		userModel.addAuthListener(frame);

		ActionListener listener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				splash.dispose();
				if (settings.getLaunchToModpacks())
					frame.selectTab("modpacks");
			}
		};
		System.out.println("DONE");

		discoverInfoPanel.setLoadListener(listener);

		LoginFrame login = new LoginFrame(resources, settings, userModel, skinRepo);
		userModel.addAuthListener(login);
		userModel.addAuthListener(new IAuthListener() {

			@Override
			public void userChanged(Object user) {
				if (user == null)
					splash.dispose();
			}
		});

		userModel.initAuth();

		Utils.sendTracking("runLauncher", "run", buildNumber.getBuildNumber(), settings.getClientId());

		try {
			ISolderPackApi pack = solder.getSolderPack(SOLDER_API, "volts-community-modpack", "");
			InstalledPack installedPack = packStore.getInstalledPacks().get(pack.getPackInfo().getName());
			ModpackModel modpack = new ModpackModel(installedPack, pack.getPackInfo(), packStore, directories);

			if (installedPack != null) {
				installer.installAndRun(resources, modpack, pack.getPackInfo().getRecommended(), installedPack == null, frame, new DownloadListener() {

					@Override
					public void stateChanged(String fileName, float progress) {
						//						progressBar.setProgress("Initializing " + fileName + "...", progress / 100);
					}
				});
			}
		} catch (RestfulAPIException e1) {
			e1.printStackTrace();
		}
	}

	public static void closeSplash() {
		splash.dispose();
	}

}
