/*
 * Copyright 2011 Karl Pauls karlpauls@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kalpatec.pojosr.framework;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.service.startlevel.StartLevel;

import de.kalpatec.pojosr.framework.felix.framework.ServiceRegistry;
import de.kalpatec.pojosr.framework.felix.framework.util.EventDispatcher;
import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;

public class PojoSR implements PojoServiceRegistry
{
	private final BundleContext m_context;
    private final ServiceRegistry m_reg = new ServiceRegistry(
            new ServiceRegistry.ServiceRegistryCallbacks()
            {
                public void serviceChanged(ServiceEvent event, Dictionary<String, Object> oldProps)
                {
                    m_dispatcher.fireServiceEvent(event, oldProps, null);
                }
            });
    private final EventDispatcher m_dispatcher = new EventDispatcher(m_reg);
    private final Map<Long, Bundle> m_bundles =new HashMap<Long, Bundle>();
    private final Map<String, Bundle> m_symbolicNameToBundle = new HashMap<String, Bundle>();
    private final Map<String, Object> bundleConfig;
    
    public PojoSR(Map<String, Object> config) throws Exception
    {
        bundleConfig = new HashMap<>(config);
        Bundle b = createPojoSRFrameworkBundle();
        m_context = b.getBundleContext();

        handleFramewoekSystemPackagesExtra(config, b);
        
        List<BundleDescriptor> scan = (List<BundleDescriptor>) config.get(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS);
        if (scan != null)
        {
            startBundles(scan);
		}
    }

	private void handleFramewoekSystemPackagesExtra(Map<String, Object> config, Bundle b) {
		Map<String,Bundle> extraPacakgesMap = new HashMap<>();
        String extraPacakges = (String) config.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        processCommaSeparatedList(b, extraPacakges, extraPacakgesMap);
        Set<String> extraPacakgesSet = extraPacakgesMap.keySet();
        for (String packageName : extraPacakgesSet) {
        	SystemPackages.addExtraPackage(packageName);
		}
	}

	private Bundle createPojoSRFrameworkBundle() throws BundleException 
	{
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "de.kalpatec.pojosr.framework");
        headers.put(Constants.BUNDLE_VERSION, "0.4.0-SNAPSHOT");
        headers.put(Constants.BUNDLE_NAME, "System Bundle");
        headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_VENDOR, "kalpatec");
		final Bundle b = new PojoSRBundle(new Revision()
        {
            @Override
            public long getLastModified()
            {
                // TODO Auto-generated method stub
                return System.currentTimeMillis();
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Enumeration getEntries()
            {
                return new Properties().elements();
            }

            @Override
            public URL getEntry(String entryName)
            {
                return getClass().getClassLoader().getResource(entryName);
            }
        }, headers, new Version(0, 0, 1), "file:pojosr", m_reg, m_dispatcher,
                null, 0, "de.kalpatec.pojosr.framework", m_bundles, getClass()
                        .getClassLoader(), bundleConfig)
        {
        	@Override
        	public synchronized void start() throws BundleException {
        		if (m_state != Bundle.RESOLVED) {
        			return;
        		}
        		m_dispatcher.startDispatching();
        		m_state = Bundle.STARTING;

                m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STARTING,
                        this));
                m_context = new PojoSRBundleContext(this, m_reg, m_dispatcher,
                                m_bundles, bundleConfig);
                int i = 0;
                for (Bundle b : m_bundles.values()) {
                	i++;
                	try
                    {
                        if (b != this)
                        {
                        	System.out.println("Starting " + i + " "+b.getSymbolicName());
                            b.start();
                        }
                    }
                    catch (Throwable t)
                    {
                    	System.out.println("Unable to start bundle: " + i + " "+b.getSymbolicName());
                    	t.printStackTrace();
                    }
                }
                m_state = Bundle.ACTIVE;
                m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STARTED,
                        this));

                m_dispatcher.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, this, null));
        		super.start();
        	};
        	
            @Override
            public synchronized void stop() throws BundleException
            {
            	if ((m_state == Bundle.STOPPING) || m_state == Bundle.RESOLVED || m_state == Bundle.INSTALLED) {
            		return;

            	}
            	else if (m_state != Bundle.ACTIVE) {
            		throw new BundleException("Can't stop pojosr because it is not ACTIVE");
            	}
            	final Bundle systemBundle = this;
            	Runnable r = new Runnable() {

					public void run() {
		                m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPING, systemBundle));
		                for (Bundle b : m_bundles.values())
		                {
		                    try
		                    {
		                        if (b != systemBundle)
		                        {
		                            b.stop();
		                        }
		                    }
		                    catch (Throwable t)
		                    {
		                        t.printStackTrace();
		                    }
		                }
		                m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPED,
		                		systemBundle));
		                m_state = Bundle.RESOLVED;
		                m_dispatcher.stopDispatching();
					}
				};
				m_state = Bundle.STOPPING;
				if (Boolean.getBoolean(PojoServiceRegistryFactory.FRAMEWORK_EVENTS_SYNC) || Boolean.valueOf((String)bundleConfig.get(PojoServiceRegistryFactory.FRAMEWORK_EVENTS_SYNC))) {
					r.run();
				}
				else {
					new Thread(r).start();
				}
            }
        };
        m_symbolicNameToBundle.put(b.getSymbolicName(), b);
        b.start();
        b.getBundleContext().registerService(StartLevel.class.getName(),
                new StartLevel()
                {

                    public void setStartLevel(int startlevel)
                    {
                        // TODO Auto-generated method stub

                    }

                    public void setInitialBundleStartLevel(int startlevel)
                    {
                        // TODO Auto-generated method stub

                    }

                    public void setBundleStartLevel(Bundle bundle,
                            int startlevel)
                    {
                        // TODO Auto-generated method stub

                    }

                    public boolean isBundlePersistentlyStarted(Bundle bundle)
                    {
                        // TODO Auto-generated method stub
                        return true;
                    }

                    public boolean isBundleActivationPolicyUsed(Bundle bundle)
                    {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    public int getStartLevel()
                    {
                        // TODO Auto-generated method stub
                        return 1;
                    }

                    public int getInitialBundleStartLevel()
                    {
                        // TODO Auto-generated method stub
                        return 1;
                    }

                    public int getBundleStartLevel(Bundle bundle)
                    {
                        // TODO Auto-generated method stub
                        return 1;
                    }
                }, null);

        b.getBundleContext().registerService(PackageAdmin.class.getName(),
                new PackageAdmin()
                {

                    public boolean resolveBundles(Bundle[] bundles)
                    {
                        // TODO Auto-generated method stub
                        return true;
                    }

                    public void refreshPackages(Bundle[] bundles)
                    {
                        m_dispatcher.fireFrameworkEvent(new FrameworkEvent(
                                FrameworkEvent.PACKAGES_REFRESHED, b, null));
                    }

                    public RequiredBundle[] getRequiredBundles(
                            String symbolicName)
                    {
                        return null;
                    }

                    public Bundle[] getHosts(Bundle bundle)
                    {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    public Bundle[] getFragments(Bundle bundle)
                    {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    public ExportedPackage[] getExportedPackages(String name)
                    {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    public ExportedPackage[] getExportedPackages(Bundle bundle)
                    {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    public ExportedPackage getExportedPackage(String name)
                    {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    public Bundle[] getBundles(String symbolicName,
                            String versionRange)
                    {
					    Bundle result = m_symbolicNameToBundle.get((symbolicName != null) ? symbolicName.trim() : symbolicName);
						if (result != null) {
							return new Bundle[] {result};
						}
						return null;
                    }

                    public int getBundleType(Bundle bundle)
                    {
                        return 0;
                    }

                    public Bundle getBundle(Class clazz)
                    {
                        return m_context.getBundle();
                    }
                }, null);
		return b;
	}

	public void startBundles(List<BundleDescriptor> scan) throws Exception 
	{
		for (BundleDescriptor desc : scan)
        {
            URL u = new URL(desc.getUrl().toExternalForm() + "META-INF/MANIFEST.MF");
            Revision r;
            if (u.toExternalForm().startsWith("file:"))
            {
                File root = new File(URLDecoder.decode(desc.getUrl().getFile(), "UTF-8"));
                u = root.toURL();
                r = new DirRevision(root);
            }
            else
            {
                URLConnection uc = u.openConnection();
                if (uc instanceof JarURLConnection)
                {
				    String target = ((JarURLConnection) uc).getJarFileURL().toExternalForm();
					String prefix = null;
					if (!("jar:" + target + "!/").equals(desc.getUrl().toExternalForm())) {
					    prefix = desc.getUrl().toExternalForm().substring(("jar:" + target + "!/").length());
					}
                    r = new JarRevision(
                            ((JarURLConnection) uc).getJarFile(),
                            ((JarURLConnection) uc).getJarFileURL(),
							prefix,
                            uc.getLastModified());
                }
                else
                {
                    r = new URLRevision(desc.getUrl(), desc.getUrl()
                            .openConnection().getLastModified());
                }
            }
            Map<String,String> bundleHeaders = desc.getManifestHeaders();
			Version osgiVersion = null;
			try {
				osgiVersion = Version.parseVersion(bundleHeaders.get(Constants.BUNDLE_VERSION));
			} catch (Exception ex) {
				ex.printStackTrace();
				osgiVersion = Version.emptyVersion;
			}
            String sym = bundleHeaders.get(Constants.BUNDLE_SYMBOLICNAME);
            if (sym != null)
            {
                int idx = sym.indexOf(';');
                if (idx > 0)
                {
                    sym = sym.substring(0, idx);
                }
				sym = sym.trim();
            }

            if (sym != null && !m_symbolicNameToBundle.containsKey( sym ))
            {
                // TODO: framework - support multiple versions
                Bundle bundle = new PojoSRBundle(r, bundleHeaders,
                        osgiVersion, desc.getUrl().toExternalForm(), m_reg,
                        m_dispatcher,
                        bundleHeaders.get(Constants.BUNDLE_ACTIVATOR),
                        m_bundles.size(),
                        sym,
                        m_bundles, desc.getClassLoader(), bundleConfig);
                if (sym != null)
                {
                	System.out.println("Found bundle "+bundle);
                    m_symbolicNameToBundle.put(bundle.getSymbolicName(), bundle);
                }
            }
        }
		resolveBundles();
        startBundles();
        
        //clean
        exportedPackages = null;
        importedPackages = null;
        requiredBundleNameFromBundle = null;
        dependencies = null;
	}

	private Map<String,Bundle> exportedPackages = new HashMap<>();
	private Map<String,Bundle> importedPackages = new HashMap<>();
	private Map<String,Bundle> requiredBundleNameFromBundle = new HashMap<>();
	private Map<Bundle,Set<Bundle>> dependencies = new HashMap<>();
	
	//puts bundles back in installed state if missing dependencies, and build dependency list for startBundle
	private void resolveBundles() {
		for (long i = 1; i < m_bundles.size(); i++)
        {
        	Bundle b = m_bundles.get(i);
            extractDependenciesAndCapabilities(b);
        }
		resolveRequiredBundles();
		matchImportedPackagesAgainstExportedPackages();
	}

	private void matchImportedPackagesAgainstExportedPackages() {
		for (Entry<String, Bundle> elem : importedPackages.entrySet()) {
			String packageName = elem.getKey();
			if (SystemPackages.containsPackage(packageName)) continue;//ignore RT.jar stuff
			PojoSRBundle bundle = (PojoSRBundle) elem.getValue();
			if (bundle.getState() != Bundle.RESOLVED) continue;
			Bundle dependBundle = exportedPackages.get(packageName);
			registerDependency(bundle, dependBundle, packageName);
		}
	}

	private void resolveRequiredBundles() {
		for (Entry<String, Bundle> elem : requiredBundleNameFromBundle.entrySet()) {
			String requiredBundleName = elem.getKey();
			PojoSRBundle bundle = (PojoSRBundle) elem.getValue();
			if (bundle.getState() != Bundle.RESOLVED) continue;
			Bundle dependBundle = m_symbolicNameToBundle.get(requiredBundleName);
			registerDependency(bundle, dependBundle, requiredBundleName);
		}
	}

	private void registerDependency(PojoSRBundle bundle, Bundle dependBundle, String requirementName) {
		
		if (dependBundle == null)
		{
			if (Boolean.valueOf((String)bundleConfig.get(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE)))
			{
				bundle.setState(Bundle.INSTALLED);
			}
			System.out.println("Unable to resolve depencies for bundle: "+bundle+" missing requirement: "+requirementName);
		}
		else if (!bundle.equals(dependBundle))
		{
			Set<Bundle> deps = dependencies.get(bundle);
			if (deps == null) {
				deps = new HashSet<>();
				dependencies.put(bundle, deps);
			}
			deps.add(dependBundle);
		}
	}
	
	private void extractDependenciesAndCapabilities(Bundle b) {
		Dictionary<String, String> headers = b.getHeaders();
		processCommaSeparatedList(b, headers.get(Constants.EXPORT_PACKAGE), exportedPackages);
		processCommaSeparatedList(b, headers.get(Constants.IMPORT_PACKAGE), importedPackages);
		processCommaSeparatedList(b, headers.get(Constants.REQUIRE_BUNDLE), requiredBundleNameFromBundle);
		//TODO:check fragmenthost? as told on spec?
	}

	private void processCommaSeparatedList(Bundle b, String mfHeaderField, Map<String, Bundle> bundleMapping) {
		if (mfHeaderField == null || mfHeaderField.trim().length() == 0) return;
		
		//we ignore version info, since we can't deal with it
		mfHeaderField = mfHeaderField.replaceAll("version=\"[^\"]*\"", "");

		String[] packageParts = mfHeaderField.split(",");
		for (String part : packageParts) {
			int idx  = part.indexOf(';');
			if (idx > 0)
			{
				//we ignore version info, since we can't deal with it
				String partOptions = part.substring(idx);
				if (partOptions.contains("optional")) continue;//ignore optional stuff
				
				part = part.substring(0,idx);
			}
			part = part.trim();
			bundleMapping.put(part,b);
		}
	}

	private void startBundles() {
		//start bundles without dependencies
		for (long i = 1; i < m_bundles.size(); i++)
        {
        	Bundle b = m_bundles.get(i);
        	if (!dependencies.containsKey(b)) startBundle(b);
        }
		
		//start bundles with least dependencies first
		List<Map.Entry<Bundle, Set<Bundle>>> sortedList = new ArrayList<>(dependencies.entrySet());
		Collections.sort(sortedList, new Comparator <Map.Entry<Bundle, Set<Bundle>>>() {
		  public int compare(Map.Entry<Bundle, Set<Bundle>> a, Map.Entry<Bundle, Set<Bundle>> b){
		    return a.getValue().size() - b.getValue().size();
		  }
		});
		for (Map.Entry<Bundle, Set<Bundle>> item : sortedList) {
			startBundle(item.getKey());
		}
	}

	private void startBundle(Bundle b) {
		try
		{
		    startBundleWithDependencies(b);
		}
		catch (Throwable e)
		{
		    System.out.println("Unable to start bundle: "+b+" with reason: "+e.getMessage());
		    Throwable cause = e.getCause();
		    if (cause != null) 
		    {
		    	cause.printStackTrace();
		    }
		}
	}

	private void startBundleWithDependencies(Bundle bundle) throws BundleException {
		if (bundle.getState() == Bundle.STARTING || bundle.getState() == Bundle.ACTIVE) return;
		else if (bundle.getState() == Bundle.RESOLVED)
		{
			//start dependend bundles first
		    Set<Bundle> deps = dependencies.get(bundle);
		    if (deps != null)
		    {
			    for (Bundle depBundle : deps) {
			    	startBundleWithDependencies(depBundle);
				}
		    }

		    System.out.println("Starting bundle: "+bundle);
		    bundle.start();
		}
		else throw new IllegalStateException("Cannot start unresolved bundle: "+bundle);
	}

	public static void main(String[] args) throws Exception
    {
    	Filter filter = null;
    	Class<?> main = null;
    	for (int i = 0;(args != null) && (i < args.length) && (i < 2); i++) {
	    	try {
	    		filter = FrameworkUtil.createFilter(args[i]);
	    	} catch (InvalidSyntaxException ie) {
	    		try {
	    			main = PojoSR.class.getClassLoader().loadClass(args[i]);
	    		} catch (Exception ex) {
	    			throw new IllegalArgumentException("Argument is neither a filter nor a class: " + args[i]);
	    		}
	    	}
    	}
        Map<String,Object> config = new HashMap<>();
        config.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS,
                (filter != null) ? new ClasspathScanner()
                        .scanForBundles(filter.toString()) : new ClasspathScanner()
                        .scanForBundles());
        new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(config);
        if (main != null) {
        	int count = 0;
        	if (filter != null) {
        		count++;
        	}
        	if (main != null) {
        		count++;
        	}
        	String[] newArgs = args;
        	if (count > 0) {
        		newArgs = new String[args.length - count];
        		System.arraycopy(args, count, newArgs, 0, newArgs.length);
        	}
        	main.getMethod("main", String[].class).invoke(null, (Object) newArgs );
        }
    }

    public BundleContext getBundleContext()
    {
        return m_context;
    }

    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException
    {
        m_context.addServiceListener(listener, filter);
    }

    public void addServiceListener(ServiceListener listener)
    {
        m_context.addServiceListener(listener);
    }

    public void removeServiceListener(ServiceListener listener)
    {
        m_context.removeServiceListener(listener);
    }

    public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties)
    {
        return m_context.registerService(clazzes, service, properties);
    }

    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties)
    {
        return m_context.registerService(clazz, service, properties);
    }

    public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        return m_context.getServiceReferences(clazz, filter);
    }

    public ServiceReference<?> getServiceReference(String clazz)
    {
        return m_context.getServiceReference(clazz);
    }

    public <S> S getService(ServiceReference<S> reference)
    {
        return m_context.getService(reference);
    }

    public boolean ungetService(ServiceReference<?> reference)
    {
        return m_context.ungetService(reference);
    }
}
