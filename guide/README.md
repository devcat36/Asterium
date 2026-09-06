# The user guide

These are upstream Stellarium's guide sources, kept so that the parts of them
this fork has to correct — `app_acknowledgements.tex` above all, which mirrors
`CREDITS.md` — stay correct rather than silently drifting out of date.

**The guide does not typeset as it stands.** Two files are not here, and both
are absent for the same reason: they derive from the Legrand Orange Book
template, which LaTeXTemplates.com publishes under CC BY-NC-SA, and a
non-commercial term has no place in a repository that is otherwise free
software end to end. The `LegrandOrangeBook` class file is the template
itself, and `guide.tex` was the template's main-file scaffold, carrying the
same CC BY-NC-SA 3.0 declaration in its header. Nothing in this fork builds
the guide and no `guide.pdf` is shipped, so removing them costs this project
nothing. The chapter and appendix sources kept here are upstream's own text
under the GNU Free Documentation License 1.3 (see `app_license.tex`).

To build the PDF anyway, restore `guide.tex` from upstream Stellarium's tree
and fetch the class from <https://www.LaTeXTemplates.com> into this
directory. What you get then is yours to use under those files' own terms,
which are not this project's.
