package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the `libs` extension.
*/
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final CommonLibraryAccessors laccForCommonLibraryAccessors = new CommonLibraryAccessors(owner);
    private final JavaxLibraryAccessors laccForJavaxLibraryAccessors = new JavaxLibraryAccessors(owner);
    private final SpringLibraryAccessors laccForSpringLibraryAccessors = new SpringLibraryAccessors(owner);
    private final TyrusLibraryAccessors laccForTyrusLibraryAccessors = new TyrusLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects) {
        super(config, providers, objects);
    }

        /**
         * Creates a dependency provider for javafaker (com.github.javafaker:javafaker)
         * This dependency was declared in settings file 'settings.gradle.kts'
         */
        public Provider<MinimalExternalModuleDependency> getJavafaker() { return create("javafaker"); }

    /**
     * Returns the group of libraries at common
     */
    public CommonLibraryAccessors getCommon() { return laccForCommonLibraryAccessors; }

    /**
     * Returns the group of libraries at javax
     */
    public JavaxLibraryAccessors getJavax() { return laccForJavaxLibraryAccessors; }

    /**
     * Returns the group of libraries at spring
     */
    public SpringLibraryAccessors getSpring() { return laccForSpringLibraryAccessors; }

    /**
     * Returns the group of libraries at tyrus
     */
    public TyrusLibraryAccessors getTyrus() { return laccForTyrusLibraryAccessors; }

    /**
     * Returns the group of versions at versions
     */
    public VersionAccessors getVersions() { return vaccForVersionAccessors; }

    /**
     * Returns the group of bundles at bundles
     */
    public BundleAccessors getBundles() { return baccForBundleAccessors; }

    /**
     * Returns the group of plugins at plugins
     */
    public PluginAccessors getPlugins() { return paccForPluginAccessors; }

    public static class CommonLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public CommonLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for common (com.herron.exchange:common)
             * This dependency was declared in settings file 'settings.gradle.kts'
             */
            public Provider<MinimalExternalModuleDependency> asProvider() { return create("common"); }

            /**
             * Creates a dependency provider for api (com.herron.exchange:common-api)
             * This dependency was declared in settings file 'settings.gradle.kts'
             */
            public Provider<MinimalExternalModuleDependency> getApi() { return create("common.api"); }

    }

    public static class JavaxLibraryAccessors extends SubDependencyFactory {
        private final JavaxJsonLibraryAccessors laccForJavaxJsonLibraryAccessors = new JavaxJsonLibraryAccessors(owner);

        public JavaxLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Returns the group of libraries at javax.json
         */
        public JavaxJsonLibraryAccessors getJson() { return laccForJavaxJsonLibraryAccessors; }

    }

    public static class JavaxJsonLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public JavaxJsonLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for json (org.glassfish:javax.json)
             * This dependency was declared in settings file 'settings.gradle.kts'
             */
            public Provider<MinimalExternalModuleDependency> asProvider() { return create("javax.json"); }

            /**
             * Creates a dependency provider for api (javax.json:javax.json-api)
             * This dependency was declared in settings file 'settings.gradle.kts'
             */
            public Provider<MinimalExternalModuleDependency> getApi() { return create("javax.json.api"); }

    }

    public static class SpringLibraryAccessors extends SubDependencyFactory {
        private final SpringBootLibraryAccessors laccForSpringBootLibraryAccessors = new SpringBootLibraryAccessors(owner);

        public SpringLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for kafka (org.springframework.kafka:spring-kafka)
             * This dependency was declared in settings file 'settings.gradle.kts'
             */
            public Provider<MinimalExternalModuleDependency> getKafka() { return create("spring.kafka"); }

        /**
         * Returns the group of libraries at spring.boot
         */
        public SpringBootLibraryAccessors getBoot() { return laccForSpringBootLibraryAccessors; }

    }

    public static class SpringBootLibraryAccessors extends SubDependencyFactory {
        private final SpringBootStarterLibraryAccessors laccForSpringBootStarterLibraryAccessors = new SpringBootStarterLibraryAccessors(owner);

        public SpringBootLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Returns the group of libraries at spring.boot.starter
         */
        public SpringBootStarterLibraryAccessors getStarter() { return laccForSpringBootStarterLibraryAccessors; }

    }

    public static class SpringBootStarterLibraryAccessors extends SubDependencyFactory {

        public SpringBootStarterLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for parent (org.springframework.boot:spring-boot-starter-parent)
             * This dependency was declared in settings file 'settings.gradle.kts'
             */
            public Provider<MinimalExternalModuleDependency> getParent() { return create("spring.boot.starter.parent"); }

            /**
             * Creates a dependency provider for web (org.springframework.boot:spring-boot-starter-web)
             * This dependency was declared in settings file 'settings.gradle.kts'
             */
            public Provider<MinimalExternalModuleDependency> getWeb() { return create("spring.boot.starter.web"); }

    }

    public static class TyrusLibraryAccessors extends SubDependencyFactory {
        private final TyrusStandaloneLibraryAccessors laccForTyrusStandaloneLibraryAccessors = new TyrusStandaloneLibraryAccessors(owner);

        public TyrusLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Returns the group of libraries at tyrus.standalone
         */
        public TyrusStandaloneLibraryAccessors getStandalone() { return laccForTyrusStandaloneLibraryAccessors; }

    }

    public static class TyrusStandaloneLibraryAccessors extends SubDependencyFactory {

        public TyrusStandaloneLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for client (org.glassfish.tyrus.bundles:tyrus-standalone-client)
             * This dependency was declared in settings file 'settings.gradle.kts'
             */
            public Provider<MinimalExternalModuleDependency> getClient() { return create("tyrus.standalone.client"); }

    }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config) { super(objects, providers, config); }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
