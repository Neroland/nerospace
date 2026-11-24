package za.co.neroland.nerospace;

public final class TestResourcePaths {
    private TestResourcePaths() {
    }

    public static String datapack(String path) {
        if (!Boolean.getBoolean("nerospace.pluralDatapackDirectories")) {
            return path;
        }
        return path.replaceFirst("^(/data/[^/]+/)(advancement|loot_table|recipe)/", "$1$2s/")
                .replaceFirst("^(/data/[^/]+/tags/)(block|entity_type|fluid|item)/", "$1$2s/");
    }
}
