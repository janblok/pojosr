package com.spectray.services;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.felix.framework.util.MapToDictionary;
import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;

public class ServicesStartupListener implements ServletContextListener 
{
	private static final long serialVersionUID	= 1L;
	private PojoServiceRegistry pojoServiceRegistry;
	private static BundleContext bundleContext;
	private ServletContext servletContext;

	@Override
	public void contextInitialized(ServletContextEvent event) 
	{
		if (bundleContext == null) 
		{
			servletContext = event.getServletContext();
			try 
			{
		        Properties props = new Properties();
		        
		        InputStream resource = servletContext.getResourceAsStream("/WEB-INF/framework.properties");
				if (resource != null) 
				{
					props.load(resource);
					resource.close();
				}
		        
				Map<String, Object> configuration = new HashMap<String, Object>();
		        for (Object key : props.keySet()) {
		        	configuration.put(key.toString(), props.get(key));
		        }
				
				List<BundleDescriptor> descriptors = new WarScanner(servletContext).scanForBundles(this.getClass().getClassLoader());
				configuration.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, descriptors);
				pojoServiceRegistry = new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(configuration);
				bundleContext = pojoServiceRegistry.getBundleContext();
				
				servletContext.setAttribute(BundleContext.class.getName(), bundleContext);
			}
			catch (Exception e) 
			{
				log("Exception in contextInitialized", e);
			}
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) 
	{
		try 
		{
			if (bundleContext != null) bundleContext.getBundle(0).stop();
		}
		catch (BundleException e) 
		{
			log("Exception in contextDestroyed", e);
		}
		finally
		{
			event.getServletContext().removeAttribute(BundleContext.class.getName());
			pojoServiceRegistry = null;
			bundleContext = null;
		}
	}
	
    private void log(String message, Throwable cause)
    {
        servletContext.log(message, cause);
    }
    
    private static class WarScanner extends ClasspathScanner
    {
    	private ServletContext servletContext;

		public WarScanner(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		public List<BundleDescriptor> scanForBundles(String filterString,ClassLoader loader) throws Exception
    	{
	        Filter filter = (filterString != null) ? FrameworkUtil.createFilter(filterString) : null;

	        loader = (loader != null) ? loader : getClass().getClassLoader();

	        Set<String> bundledJars = findBundles();
	        List<BundleDescriptor> bundles = new ArrayList<BundleDescriptor>();
	        for (Enumeration<URL> e = loader.getResources("META-INF/MANIFEST.MF"); e.hasMoreElements();)
	        {
	            URL manifestURL = e.nextElement();
	            String jarName = manifestURL.toString();
	            int idx = jarName.indexOf('!');
	            if (idx == -1) continue;
	            jarName = jarName.substring(0, idx);
	            idx = jarName.lastIndexOf('/');
	            if (idx == -1) continue;
	            jarName = jarName.substring(idx+1);
	            if (bundledJars.contains(jarName))
	            {
		            Map<String, String> headers = getManifestHeaders(manifestURL);
		            if (headers.containsKey(Constants.BUNDLE_SYMBOLICNAME))
		            {
			            if ((filter == null) || filter.match(new MapToDictionary<String,String>(headers)))
			            {
			            	bundles.add(new BundleDescriptor(loader, getParentURL(manifestURL), headers));
			            }
		            }
	            }
	        }
	        return bundles;
    	}
    	
	    private Set<String> findBundles() throws Exception
	    {
	        Set<String> list = new HashSet<String>();
	        for (Object o : servletContext.getResourcePaths("/WEB-INF/lib/")) {
	            String name = (String)o;
	            if (name.endsWith(".jar")) {
	                URL url = this.servletContext.getResource(name);
	                if (url != null) {
	                	String jarNames = url.toString();
	    	            int idx = jarNames.lastIndexOf('/');
	    	            if (idx == -1) continue;
	    	            jarNames = jarNames.substring(idx+1);
	                    list.add(jarNames);
	                }
	            }
	        }
	
	        return list;
	    }
    }
}
