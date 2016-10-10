package de.whitefrog.neobase.patch;

import com.github.zafarkhaja.semver.Version;
import de.whitefrog.neobase.Service;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Transaction;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;

public class Patcher {
    private static final Logger logger = LoggerFactory.getLogger(Patcher.class);
    private static final String snapshotSuffix = "-SNAPSHOT";
    private static String version;

    private static TreeMap<Version, List<Patch>> getPatches(Service service) {
        Reflections reflections = new Reflections(Patch.class.getPackage().getName());
        Set<Class<? extends Patch>> patchClasses = reflections.getSubTypesOf(Patch.class);
        TreeMap<Version, List<Patch>> patches = new TreeMap<>();

        for(Class<? extends Patch> patchClass : patchClasses) {
            try {
                Constructor<? extends Patch> constructor = patchClass.getConstructor(Service.class);
                Patch patch = constructor.newInstance(service);

                if(patches.containsKey(patch.getVersion())) {
                    patches.get(patch.getVersion()).add(patch);
                } else {
                    List<Patch> patchList = new ArrayList<>();
                    patchList.add(patch);
                    patches.put(patch.getVersion(), patchList);
                }
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return patches;
    }

    public synchronized static String getVersion() {
        if(version != null) return version;

        version = System.getProperty("version", Patcher.class.getPackage().getImplementationVersion());

        if(version == null) {
            version = "undefined";
            logger.warn("No implementation version found in manifest");
        } else {
            if(version.endsWith(snapshotSuffix)) version = version.replace(snapshotSuffix, StringUtils.EMPTY);
        }

        return version;
    }

    public static void patch(Service service) {
        String version = getVersion();
        if(!version.equals("undefined")) patch(service, getVersion());
    }

    public static void patch(Service service, String versionString) {
        Version version = Version.valueOf(versionString);
        Transaction tx = service.beginTx();

        Version graphVersion = Version.valueOf(service.getVersion());
        logger.info("Graph version is {}", graphVersion);

        tx.success();
        tx.close();

        if(graphVersion.greaterThan(version)) {
            logger.warn("API version is {}, which is less than the graph version", version);
            Map<Version, List<Patch>> patches = getPatches(service).subMap(version, false, graphVersion, true);
            if(patches.isEmpty()) logger.warn("Don't worry, no patches necessary");
            else logger.warn("{} patches found, API update required!!!", patches.size());
        } else {
            if(!graphVersion.equals(version)) {
                Map<Version, List<Patch>> patches = getPatches(service).subMap(graphVersion, false, version, true);
                if(!patches.isEmpty()) {
                    logger.info("Graph version differs ({} -> {})", graphVersion, version);
                    for(Version patchVersion : patches.keySet()) {
                        logger.info("Applying patches for {}", patchVersion);
                        List<Patch> patchList = patches.get(patchVersion);
                        Collections.sort(patchList);
                        for(Patch patch : patchList) {
                            patch.setService(service);
                            try {
                                logger.info("    Applying {}Patch", patch.getClass().getSimpleName());
                                patch.update();
                            } catch(Exception e) {
                                logger.error("   {}Patch failed: {}", patch.getClass().getSimpleName(), e.getMessage(), e);
                                logger.error("Shutting down!");
                                System.exit(1);
                            }
                        }
                        try(Transaction updateTx = service.beginTx()) {
                            logger.info("Patches {} -> {} applied", graphVersion, patchVersion);
                            service.setVersion(patchVersion.getNormalVersion());
                            updateTx.success();
                        }
                    }
                    try(Transaction updateTx = service.beginTx()) {
                        service.setVersion(version.getNormalVersion());
                        updateTx.success();
                    }
                } else {
                    try(Transaction updateTx = service.beginTx()) {
                        logger.info("Graph version differs ({} -> {}), no patches available", graphVersion, version);
                        service.setVersion(version.getNormalVersion());
                        updateTx.success();
                    }
                }
            }
        }
    }
}
