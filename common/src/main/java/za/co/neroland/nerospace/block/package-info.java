/**
 * Generic, loader-free block base classes for the 2.0 planet and station blocks (decision 002, rework Stage 2).
 * Stage 3's planet packages instantiate these with their own {@code Properties}; every behaviour delegates to a pure
 * helper in {@link za.co.neroland.nerospace.block.rules}. Server-side work is event-, random-tick- or scheduled-tick
 * driven with the per-chunk caps from 002 §3/§4; nothing here scans every tick.
 */
package za.co.neroland.nerospace.block;
