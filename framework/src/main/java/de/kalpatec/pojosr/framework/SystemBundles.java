package de.kalpatec.pojosr.framework;

import java.util.HashSet;
import java.util.Set;

public class SystemBundles {

    private static final Set<String> CS = new HashSet<String>();

    public static void addExtraBundle(String bundleName)
    {
    	CS.add(bundleName);
    }
    
    public static boolean containsBundle(String o) {
        return CS.contains(o);
    }

    private SystemBundles() {}
}

