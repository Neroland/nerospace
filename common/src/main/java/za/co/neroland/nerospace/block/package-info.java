/**
 * Generic, loader-free block base classes for the 2.0 planet and station blocks.
 * The planet packages instantiate these with their own {@code Properties}; every behaviour delegates to a pure
 * helper in {@link za.co.neroland.nerospace.block.rules}. Server-side work is event-, random-tick- or scheduled-tick
 * driven with the per-chunk caps; nothing here scans every tick.
 */
package za.co.neroland.nerospace.block;
