/*
 * Copyright 2009 NCHOVY
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.araqne.bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.araqne.api.BundleManager;
import org.araqne.api.BundleRepository;
import org.araqne.api.BundleStatus;
import org.araqne.api.KeyStoreManager;
import org.araqne.api.MavenArtifact;
import org.araqne.api.MavenResolveException;
import org.araqne.api.ProgressMonitor;
import org.araqne.api.Version;
import org.araqne.confdb.ConfigService;
import org.araqne.pkg.MavenResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides all OSGi bundle related functions.
 * 
 * @author xeraph
 * 
 */
public class BundleManagerService implements SynchronousBundleListener, BundleManager {
	private Logger logger = LoggerFactory.getLogger(BundleManagerService.class);

	private BundleContext context;
	private BundleConfig config;

	public BundleManagerService(BundleContext bc) {
		this.context = bc;
		bc.addBundleListener(this);
		config = new BundleConfig(getConfigService());
		installPlugins();
	}

	private void installPlugins() {
		File dir = new File(System.getProperty("araqne.plugin.dir"));
		if (!dir.exists() || !dir.isDirectory())
			return;

		for (File f : dir.listFiles()) {
			if (!f.getName().endsWith(".jar"))
				continue;

			try {
				Bundle b = findBundle(f);
				if (b != null)
					continue;
			} catch (IOException e) {
			}

			long id = -1;
			try {
				logger.info("araqne core: installing plugin bundle [{}]", f.getAbsolutePath());

				String filePath = f.getAbsolutePath();
				String os = System.getProperty("os.name");
				if (os != null && os.contains("Windows"))
					filePath = "/" + filePath;

				id = installBundle("file://" + filePath);
			} catch (Throwable t) {
				logger.error("araqne core: cannot install plugin bundle [" + f.getAbsolutePath() + "]", t);
				continue;
			}

			try {
				logger.info("araqne core: starting plugin bundle [{}:{}]", id, f.getAbsolutePath());
				startBundle(id);
			} catch (Throwable t) {
				logger.error("araqne core: cannot start plugin bundle [" + id + ":" + f.getAbsolutePath() + "]", t);
			}
		}
	}

	private Bundle findBundle(File f) throws IOException {
		JarFile jar = null;
		try {
			jar = new JarFile(f);
			Manifest manifest = jar.getManifest();
			Attributes attrs = manifest.getMainAttributes();
			String symbolicName = attrs.getValue("Bundle-SymbolicName");

			for (Bundle b : context.getBundles()) {
				if (b.getSymbolicName().equals(symbolicName))
					return b;
			}
			return null;
		} finally {
			if (jar != null)
				jar.close();
		}
	}

	private ConfigService getConfigService() {
		ServiceReference<?> ref = context.getServiceReference(ConfigService.class.getName());
		return (ConfigService) context.getService(ref);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#refresh()
	 */
	@Override
	public void refresh() {
		ServiceReference<?> ref = context.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) context.getService(ref);
		packageAdmin.refreshPackages(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#getRemoteRepositories()
	 */
	@Override
	public List<BundleRepository> getRemoteRepositories() {
		return config.getRepositories();
	}

	@Override
	public List<BundleRepository> getRepositories() {
		return config.getRepositories();
	}

	@Override
	public BundleRepository getRepository(String alias) {
		return config.getRepository(alias);
	}

	@Override
	public void addRepository(BundleRepository repo) {
		config.addRepository(repo);
	}

	@Override
	public void updateRepository(BundleRepository repo) {
		config.updateRepository(repo);
	}

	@Override
	public void removeRepository(String alias) {
		config.removeRepository(alias);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.araqne.bundle.BundleManager#addRemoteRepository(java.lang.String,
	 * java.net.URL)
	 */
	@Override
	public void addRemoteRepository(String alias, URL url) {
		config.addRepository(new BundleRepository(alias, url, 0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#addSecureRemoteRepository(java.lang
	 * .String, java.net.URL, java.lang.String, java.lang.String)
	 */
	@Override
	public void addSecureRemoteRepository(String alias, URL url, String trustStoreAlias, String keyStoreAlias) {
		BundleRepository repo = new BundleRepository(alias, url);
		repo.setTrustStoreAlias(trustStoreAlias);
		repo.setKeyStoreAlias(keyStoreAlias);
		config.addRepository(repo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#removeRemoteRepository(java.lang.
	 * String)
	 */
	@Override
	public void removeRemoteRepository(String alias) {
		config.removeRepository(alias);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#installBundle(java.lang.String)
	 */
	@Override
	public long installBundle(String filePath) {
		try {
			if (!isBundleJar(new File(filePath.substring("file://".length()))))
				throw new IllegalStateException("invalid OSGi bundle: " + filePath);

			Bundle bundle = context.installBundle(filePath);
			return bundle.getBundleId();
		} catch (BundleException e) {
			throw new IllegalStateException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#installBundle(org.araqne.pkg.
	 * ProgressMonitor, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public long installBundle(ProgressMonitor monitor, String groupId, String artifactId, String version)
			throws MavenResolveException {
		MavenResolver resolver = new MavenResolver(getLocalRepository(), config.getRepositories(), monitor, getKeyStoreManager());
		Version v = (version != null) ? new Version(version) : null;
		MavenArtifact artifact = new MavenArtifact(groupId, artifactId, v);

		if (isBuiltinArtifact(artifact.getGroupId(), artifact.getArtifactId()))
			throw new IllegalStateException("provided in system bundle");

		if (monitor != null)
			monitor.writeln(String.format("Resolving %s/%s", artifact.getGroupId(), artifact.getArtifactId())
					+ (artifact.getVersion() != null ? (" (" + artifact.getVersion() + ")") : ""));

		File file = resolver.resolve(artifact);
		if (!isBundleJar(file))
			throw new IllegalStateException("invalid OSGi bundle: " + groupId + "/" + artifactId + " (" + version + ")");

		String filePath = file.getAbsolutePath();

		try {
			Bundle newBundle = context.installBundle(getPrefix() + filePath.replace('\\', '/'));
			if (newBundle.getSymbolicName() == null)
				newBundle.uninstall();

			return newBundle.getBundleId();
		} catch (BundleException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean isBundleJar(File f) {
		if (!f.isFile() || !f.exists())
			return false;

		JarFile jar = null;
		try {
			jar = new JarFile(f);
			if (jar.getManifest() == null || jar.getManifest().getMainAttributes() == null)
				return false;

			return jar.getManifest().getMainAttributes().getValue("Bundle-SymbolicName") != null;
		} catch (IOException e) {
			throw new IllegalArgumentException("cannot check OSGi bundle manifest: " + f.getAbsolutePath());
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private String getPrefix() {
		String prefix = "file://";
		if (File.separatorChar != '/')
			prefix += "/";
		return prefix;
	}

	private File getLocalRepository() {
		return new File(getDownloadRoot());
	}

	private String getDownloadRoot() {
		return new File(System.getProperty("araqne.download.dir")).getAbsolutePath().replaceAll("\\\\", "/");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#uninstallBundle(long)
	 */
	@Override
	public boolean uninstallBundle(long bundleId) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null) {
			logger.warn(String.format("bundle %d not found.", bundleId));
			return false;
		}

		try {
			String downloadRoot = getDownloadRoot();

			String prefix = getPrefix();

			File bundleLocation = new File(bundle.getLocation().replace(prefix, ""));

			// prevents destruction out of the download directory.
			// delete cached bundle in download directory for redownloading
			if (bundle.getLocation().startsWith(prefix + downloadRoot)) {
				File bundleDirectory = new File(bundleLocation.getParent());
				bundleLocation.delete();
				deleteDirectory(bundleDirectory);
			}
			bundle.uninstall();
			return true;
		} catch (BundleException e) {
			logger.error(e.getMessage());
			return false;
		}
	}

	/**
	 * Delete the directory recursively.
	 * 
	 * @param path
	 *            the directory will be removed
	 * @return true if and only if the directory is successfully deleted
	 */
	private boolean deleteDirectory(File path) {
		if (path.exists()) {
			for (File file : path.listFiles()) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}

		logger.info("deleting " + path.getAbsolutePath());
		return path.delete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#startBundle(long)
	 */
	@Override
	public void startBundle(long bundleId) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null) {
			logger.warn(String.format("bundle %d not found", bundleId));
			throw new IllegalStateException("bundle " + bundleId + " not found");
		}

		try {
			bundle.start();
		} catch (BundleException e) {
			throw new IllegalStateException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#stopBundle(long)
	 */
	@Override
	public void stopBundle(long bundleId) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null) {
			logger.warn(String.format("bundle %d not found", bundleId));
			throw new IllegalStateException("bundle " + bundleId + " not found");
		}

		logger.info("Stopping Bundle " + bundle.getSymbolicName());
		try {
			bundle.stop();
		} catch (BundleException e) {
			logger.error("araqne core: stopping bundle failed.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#updateBundle(long)
	 */
	@Override
	public void updateBundle(long bundleId) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null) {
			logger.warn(String.format("bundle %d not found", bundleId));
			throw new IllegalStateException("bundle " + bundleId + " not found");
		}

		try {
			if (!isLocalJar(bundle)) {
				try {
					File before = new File(bundle.getLocation().replace("file://", ""));
					if (before.exists()) {
						File temp = File.createTempFile(before.getName(), "", before.getParentFile());
						temp.delete();
						if (before.renameTo(temp)) {
							MavenResolver resolver = new MavenResolver(getLocalRepository(), config.getRepositories(), null,
									getKeyStoreManager());
							MavenArtifact artifact = getArtifact(bundle);
							File after = resolver.resolve(artifact);
							if (after.exists())
								temp.delete();
							else
								temp.renameTo(before);
						}
					} else {
						before.getParentFile().mkdirs();
						MavenResolver resolver = new MavenResolver(getLocalRepository(), config.getRepositories(), null,
								getKeyStoreManager());
						MavenArtifact artifact = getArtifact(bundle);
						resolver.resolve(artifact);
					}
				} catch (MavenResolveException e) {
					logger.error("araqne core: maven resolve failed.", e);
					throw new RuntimeException("maven resolve failed. (" + e.getMessage() + ")");
				} catch (IOException e) {
					logger.error("araqne core: create temp file failed.", e);
					throw new RuntimeException("create temp file failed.");
				}
			}
			bundle.update();
		} catch (BundleException e) {
			logger.error("araqne core: updating bundle failed.", e);
		}
	}

	@Override
	public void updateBundle(long bundleId, String bundleLocation) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null) {
			logger.warn(String.format("bundle %d not found", bundleId));
			throw new IllegalStateException("bundle " + bundleId + " not found");
		}

		try {
			File file = new File(new URI(bundleLocation));
			if (!isBundleJar(file))
				throw new IllegalStateException("invalid OSGi bundle: " + bundleLocation);

			bundle.update(new FileInputStream(file));
		} catch (Exception e) {
			throw new IllegalStateException("bundleId: " + bundleId + ", location: " + bundleLocation, e);
		}
	}

	@Override
	public void updateBundleVersion(long bundleId, String groupId, String artifactId, String version) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null) {
			logger.warn(String.format("bundle %d not found", bundleId));
			throw new IllegalStateException("bundle " + bundleId + " not found");
		}

		MavenResolver resolver = new MavenResolver(getLocalRepository(), config.getRepositories(), null, getKeyStoreManager());
		Version v = (version != null) ? new Version(version) : null;
		MavenArtifact artifact = new MavenArtifact(groupId, artifactId, v);

		if (isBuiltinArtifact(artifact.getGroupId(), artifact.getArtifactId()))
			throw new IllegalStateException("provided in system bundle");

		try {
			File file = resolver.resolve(artifact);
			if (!isBundleJar(file))
				throw new IllegalStateException("invalid OSGi bundle: " + groupId + "/" + artifactId + " (" + version + ")");

			bundle.update(new FileInputStream(file));
		} catch (Exception e) {
			throw new IllegalStateException("bundleId: " + bundleId + ", version: " + version, e);
		}
	}

	private boolean isLocalJar(Bundle bundle) {
		File location = new File(bundle.getLocation().replace("file://", ""));
		File araqneDownload = new File(System.getProperty("araqne.download.dir"));

		while (location.getParentFile() != null) {
			if (location.equals(araqneDownload))
				return false;
			location = location.getParentFile();
		}

		return true;
	}

	private MavenArtifact getArtifact(Bundle bundle) {
		File location = new File(bundle.getLocation().replace("file://", ""));
		File araqneDownload = new File(System.getProperty("araqne.download.dir"));

		String groupId = null;
		String artifactId = null;
		Version version = null;
		while (location.getParentFile() != null) {
			location = location.getParentFile();
			if (location.equals(araqneDownload))
				break;

			String name = location.getName();
			if (version == null)
				version = new Version(name);
			else if (artifactId == null)
				artifactId = name;
			else if (groupId == null)
				groupId = name;
			else
				groupId = name + "." + groupId;
		}

		return new MavenArtifact(groupId, artifactId, version);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#getBundles()
	 */
	@Override
	public Map<Long, BundleStatus> getBundles() {
		Map<Long, BundleStatus> bundles = new HashMap<Long, BundleStatus>();
		for (Bundle bundle : context.getBundles()) {
			String version = getBundleVersion(bundle);
			Date packagedDate = null;
			Object object = bundle.getHeaders().get("Bnd-LastModified");
			if (object != null)
				packagedDate = new Date(Long.parseLong((String) object));
			BundleStatus bundleStatus = new BundleStatus(bundle.getSymbolicName(), version, bundle.getState(), packagedDate);
			bundles.put(bundle.getBundleId(), bundleStatus);
		}
		return bundles;
	}

	private String getBundleVersion(Bundle bundle) {
		return (String) bundle.getHeaders().get("Bundle-Version");
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		if (event.getType() == BundleEvent.STARTED) {
			logger.info(String.format("Starting %s [%d] bundle.", bundle.getSymbolicName(), bundle.getBundleId()));
		} else if (event.getType() == BundleEvent.STOPPED) {
			logger.info(String.format("Stopping %s [%d] bundle.", bundle.getSymbolicName(), bundle.getBundleId()));
		}
	}

	public boolean isBuiltinArtifact(String groupId, String artifactId) {
		if (groupId.equals("org.apache.felix") && artifactId.startsWith("org.osgi."))
			return true;

		if (groupId.equals("org.apache.felix") && artifactId.equals("org.apache.felix.main"))
			return true;

		if (groupId.equals("org.araqne") && artifactId.equals("araqne-api"))
			return true;

		if (groupId.equals("org.slf4j") && artifactId.equals("slf4j-api"))
			return true;

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.araqne.bundle.BundleManager#getBundleLocation(long)
	 */
	@Override
	public String getBundleLocation(long bundleId) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null)
			throw new IllegalStateException("bundle not found: " + bundleId);

		return bundle.getLocation();
	}

	@Override
	public List<String> getEntryPaths(long bundleId, String directory) {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null)
			throw new IllegalStateException("bundle not found: " + bundleId);

		List<String> paths = new ArrayList<String>();
		Enumeration<String> e = bundle.getEntryPaths(directory);
		if (e == null) {
			return paths;
		}

		while (e.hasMoreElements()) {
			paths.add(e.nextElement());
		}

		return paths;
	}

	@Override
	public String getEntry(long bundleId, String path) throws IOException {
		Bundle bundle = context.getBundle(bundleId);
		if (bundle == null)
			throw new IllegalStateException("bundle not found: " + bundleId);

		URL url = bundle.getEntry(path);
		if (url == null)
			throw new FileNotFoundException(path);

		InputStream is = url.openStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));

		StringBuilder sb = new StringBuilder(4096);
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;

			sb.append(line);
			sb.append("\n");
		}

		return sb.toString();
	}

	private KeyStoreManager getKeyStoreManager() {
		ServiceReference<?> ref = context.getServiceReference(KeyStoreManager.class.getName());
		KeyStoreManager keyman = (KeyStoreManager) context.getService(ref);
		return keyman;
	}

	@Override
	public boolean isLocallyInstalledBundle(long bundleId) {
		String bundleLocation = getBundleLocation(bundleId);
		if (bundleLocation.equals("System Bundle"))
			return false;

		File bundleFile = null;
		try {
			bundleFile = new File(new URI(bundleLocation));
			return !bundleFile.getAbsolutePath().startsWith(getLocalRepository().getAbsolutePath());
		} catch (URISyntaxException e) {
			return false;
		}
	}

}
