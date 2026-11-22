# Handover for NeroCompanion — Suit Tender

Idea only. Nerospace does not need a new API for it.

A "Suit Tender" chip can top up the owner's worn Pathfinder chest by calling Neroland Core `ItemEnergyLookup.find` on `nerospace:suit_chestplate` and inserting FE. The suit does not store an owner. The chip should only touch the suit the owner is wearing, and it should not write a player id onto the item.

Nerospace quest data for the suit lives in this mod (`breathe_easy` now collects the four `suit_*` pieces). No NeroCompanion code was changed.
