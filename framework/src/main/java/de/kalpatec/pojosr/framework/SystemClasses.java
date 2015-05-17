package de.kalpatec.pojosr.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class SystemClasses {

    private static Set<String> CS = new HashSet<String>();

    static {
        try {
            File file = new File(System.getProperty("java.home"), "lib/classlist");
            BufferedReader r = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = r.readLine()) != null) {
            	//we are only intrested in packages...not classnames
            	int idx = line.lastIndexOf('/');
            	if (idx > 0) line = line.substring(idx);
            	
            	CS.add(line.replace('/', '.'));
            }
        } catch (Exception e) {
            //fail silently... throw new RuntimeException(e);
        }
    }

    public static boolean containsPackage(String o) {
        return CS.contains(o) || o.startsWith("java") || o.startsWith("com.sun")
                || o.startsWith("sun") || o.startsWith("oracle")
                || o.startsWith("org.xml") || o.startsWith("org.w3c.dom") 
                || o.startsWith("javax.security") || o.startsWith("javax.sound") || o.startsWith("javax.swing") 
                || o.startsWith("javax.accessibility") || o.startsWith("com.oracle");
    }

    private SystemClasses() {
    }
}

