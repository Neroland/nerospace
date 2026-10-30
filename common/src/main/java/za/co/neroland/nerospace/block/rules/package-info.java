/**
 * Pure, side-effect-free block behaviour rules for the 2.0 planet and station blocks.
 * Every class here is plain Java: no {@code Level}, no registry access, no ticking. Block classes in
 * {@code za.co.neroland.nerospace.block} gather the world facts and ask these helpers what to do, so each
 * rule — and every per-chunk cap — is covered by a JUnit test without a server.
 */
package za.co.neroland.nerospace.block.rules;
