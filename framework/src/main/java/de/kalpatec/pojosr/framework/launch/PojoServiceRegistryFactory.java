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
package de.kalpatec.pojosr.framework.launch;

import java.util.Map;

public interface PojoServiceRegistryFactory 
{
	public static final String FRAMEWORK_EVENTS_SYNC = "de.kalpatec.pojosr.framework.events.sync";
    public static final String BUNDLE_DESCRIPTORS = PojoServiceRegistry.class.getName().toLowerCase() + ".bundles";

    /**
     * Options are:
     * Pass BUNDLE_DESCRIPTORS with a List of BundleDescriptor retrieved from classpath scanners or leave empty to have PojoSR so this
     * When in the configuration Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE=false is used, all bundle checks are disabled
     * For synchronized loading pass FRAMEWORK_EVENTS_SYNC=true (or use as system property)
     * 
     * @param configuration
     * @return the instance
     * @throws Exception
     */
    public PojoServiceRegistry newPojoServiceRegistry(Map<String,Object> configuration) throws Exception;
}
