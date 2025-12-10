package net.mellow.nbtlib.api.format;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public class StructureProviderRegistry {

    private static Map<String, IStructureProvider> providerMap = new HashMap<>();

    public static void init() {
        registerFormat(new NBTFormatProvider());
    }

    public static void registerFormat(IStructureProvider provider) {
        providerMap.put(provider.getFileExtension(), provider);
    }

    public static IStructureProvider getFormatFor(String filename) {
        return providerMap.get(FilenameUtils.getExtension(filename));
    }

}
