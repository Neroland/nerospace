# Translated wiki pages

This folder holds translations of the English wiki pages in `wiki/`.

## Layout

```
wiki/
  <Page>.md              English source (authoritative)
  i18n/
    zh_cn/<Page>.md      Simplified Chinese
    it_it/<Page>.md      Italian
```

- Each translated page keeps the **same file name** as its English source.
- Links between wiki pages point to the translated page in the same language folder. Links to
  the English page, images and repository docs use `../../` to climb back to `wiki/`.
- Markdown structure, code blocks, commands, config keys, item IDs, image paths and URLs are
  identical to the English page. Only prose, table text and link text are translated.
- Brand, mod, material and planet names stay in English (Neroland, Nerospace, Nerosium,
  Cindara, ...).

## Status: drafts pending review

Every page here is a **machine-translated draft**. Each one starts with a one-line notice that
the English page is authoritative and links back to it. The drafts will be uploaded to Crowdin
as unapproved suggestions and only count as reviewed once a native-speaker proofreader approves
them there.

Pages about privacy and data are translated as a **summary only**, with a notice that the
English version is the binding text. Read the English page for the full details.

When an English page changes, the matching translations go stale until Crowdin (or a
contributor) updates them. If in doubt, trust the English page.

## Adding a language

1. Create `wiki/i18n/<locale>/` using the Minecraft locale code in lowercase
   (for example `de_de`, `pt_br`).
2. Add one file per English page, with the same file name, starting with the
   "English is authoritative" notice in that language and a link to `../../<Page>.md`.
3. Keep code blocks, commands, IDs and URLs unchanged, and point wiki links at pages in the
   same folder.
4. If the repository has a `crowdin.yml`, add the language mapping there so Crowdin writes to
   the same folder.

The English page set is the source of truth: a translation folder should contain exactly the
English pages (the sidebar is not translated).
