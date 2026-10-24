package za.co.neroland.nerospace.block.light;

/** Implemented by every light engine through {@code LightEngineNyxMixin}: the engine's own {@link NyxLightIndex}. */
public interface NyxIndexHolder {

    NyxLightIndex nerospace$nyxIndex();
}
