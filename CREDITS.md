## ABOUT THIS BUILD

This is Asterium, a modified version of Stellarium: a fork that replaces the
desktop interface with a touch-first Android one and repackages the data tree
inside an APK. Everything credited below is credited as Stellarium ships it
unless an entry says otherwise. The full list of changes against upstream is
in NOTICE.md in the source, published at
https://github.com/devcat36/Asterium. Asterium is not affiliated with or
endorsed by the Stellarium project.

## LICENSE
```
   Copyright (C) 2004-2026 Fabien Chereau et al.
   Copyright (C) 2026 the Asterium authors

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA  02110-1335, USA.

   The application as built and distributed is a combined work with
   libraries offered only under version 3 of the GNU General Public
   License (Qt Charts among them) and with LGPL-3.0 and Apache-2.0
   components; it is therefore delivered to you under the GNU General
   Public License, version 3.

   See the COPYING and COPYING.GPL3 files for more information regarding the
   GNU General Public License, and COPYING.LGPL3, COPYING.LGPL21 and
   COPYING.APACHE2 for the other terms named above. All five ship inside the
   application and are readable from its Help sheet.
```

## Full Reference & Credits
```
1. Technical Articles
	1.1 The tone reproductor class
		The class mainly performs a fast implementation of the algorithm
		from the paper [1], with more accurate values from [2]. The blue
		shift formula is taken from [3] and combined with the Scotopic
		vision formula from [4].
		[1] "Tone Reproduction for Realistic Images", Tumblin and
		    Rushmeier, IEEE Computer Graphics & Application, November
		    1993
		[2] "Tone Reproduction and Physically Based Spectral Rendering",
		    Devlin, Chalmers, Wilkie and Purgathofer in EUROGRAPHICS
		    2002
		[3] "Night Rendering", H. Wann Jensen, S. Premoze, P. Shirley,
		    W.B. Thompson, J.A. Ferwerda, M.M. Stark
		[4] "A Visibility Matching Tone Reproduction Operator for High
		    Dynamic Range Scenes", G.W. Larson, H. Rushmeier, C. Piatko
	1.2 The skylight class
		The class is a fast implementation of the algorithm from the
		article "A Practical Analytic Model for Daylight" by A. J.
		Preetham, Peter Shirley and Brian Smits.
	1.3 The skybright class
		The class is a fast reimplementation of the VISLIMIT.BAS basic
		source code from Brad Schaefer's article on pages 57-60,  May
		1998 _Sky & Telescope_,	"To the Visual Limits". The basic
		sources are available on the Sky and Telescope web site.
		(code "offered as-is and without support.")
	1.4 The Delta-T calculations
		For implementation of calculation routines for Delta-T we used 
		the following sources:
		[1] Delta-T webpage by Rob van Gent: 
		    http://www.staff.science.uu.nl/~gent0113/deltat/deltat.htm
		[2] "Five Millennium Canon of Solar Eclipses", Espenak and Meeus
		    http://eclipse.gsfc.nasa.gov/SEhelp/deltatpoly2004.html
		[3] "On the system of astronomical constants", Clemence, G. M.,
		    Astronomical Journal, Vol. 53, p. 169
		    http://adsabs.harvard.edu/abs/1948AJ.....53..169C
		[4] "The Rotation of the Earth, and the Secular Accelerations of
		    the Sun, Moon and Planets",
		    Spencer Jones, Monthly Notices of the Royal Astronomical 
		    Society, 99 (1939), 541-558
		    http://adsabs.harvard.edu/abs/1939MNRAS..99..541S
		[5] "Polynomial approximations for the correction delta T E.T.-
		    U.T. in the period 1800-1975",
		    Schmadel, L. D.; Zech, G., Acta Astronomica, vol. 29, no. 1,
		    1979, p. 101-104.
		    http://adsabs.harvard.edu/abs/1979AcA....29..101S
		[6] "ELP 2000-85 and the dynamic time-universal time relation",
		    Borkowski, K. M.,
		    Astronomy and Astrophysics (ISSN 0004-6361), vol. 205, no. 
		    1-2, Oct. 1988, p. L8-L10.
		    http://adsabs.harvard.edu/abs/1988A&A...205L...8B
		[7] "Empirical Transformations from U.T. to E.T. for the Period
		    1800-1988", 
		    Schmadel, L. D.; Zech, G., Astronomische Nachrichten 309, 
		    219-221
		    http://adsabs.harvard.edu/abs/1988AN....309..219S
		[8] "Historical values of the Earth's clock error DeltaT and the
		    calculation of eclipses",
		    Morrison, L. V.; Stephenson, F. R., Journal for the History
		    of Astronomy (ISSN 0021-8286), 
		    Vol. 35, Part 3, No. 120, p. 327 - 336 (2004)
		    http://adsabs.harvard.edu/abs/2004JHA....35..327M
		[9] "Addendum: Historical values of the Earth's clock error", 
		    Morrison, L. V.; Stephenson, F. R.,
		    Journal for the History of Astronomy (ISSN 0021-8286), Vol.
		    36, Part 3, No. 124, p. 339 (2005)
		    http://adsabs.harvard.edu/abs/2005JHA....36..339M
		[10] "Polynomial approximations to Delta T, 1620-2000 AD", 
		    Meeus, J.; Simons, L.,
		    Journal of the British Astronomical Association, vol.110,
		    no.6, 323
		    http://adsabs.harvard.edu/abs/2000JBAA..110..323M
		[11] "Einstein's Theory of Relativity Confirmed by Ancient Solar
		    Eclipses", Henriksson G.,
		    http://adsabs.harvard.edu/abs/2009ASPC..409..166H
		[12] "Canon of Solar Eclipses" by Mucke & Meeus (1983)
		[13] "The accelerations of the earth and moon from early
		     astronomical observations", 
		     Muller P. M., Stephenson F. R.,
		     http://adsabs.harvard.edu/abs/1975grhe.conf..459M
		[14] "Pre-Telescopic Astronomical Observations", Stephenson F.R.,
		    http://adsabs.harvard.edu/abs/1978tfer.conf....5S
		[15] "Long-term changes in the rotation of the earth - 700 B.C.
		     to A.D. 1980", 
		     Stephenson F. R., Morrison L. V., 
		     Philosophical Transactions, Series A (ISSN 0080-4614), vol.
		     313, no. 1524, Nov. 27, 1984, p. 47-70.
		     http://adsabs.harvard.edu/abs/1984RSPTA.313...47S
		[16] "Long-Term Fluctuations in the Earth's Rotation: 700 BC to
		    AD 1990",
		    Stephenson F. R., Morrison L. V., 
		    Philosophical Transactions: Physical Sciences and 
		    Engineering, Volume 351, Issue 1695, pp. 165-202
		    http://adsabs.harvard.edu/abs/1995RSPTA.351..165S
		[17] "Historical Eclipses and Earth's Rotation" by F. R. 
		    Stephenson (1997)
		    http://ebooks.cambridge.org/ebook.jsf?bid=CBO9780511525186
		[18] "Astronomical Algorithms" by J. Meeus (2nd ed., 1998)
		[19] "Astronomy on the Personal Computer" by O. Montenbruck & 
		     T. Pfleger (4nd ed., 2000)
		[20] "Calendrical Calculations" by E. M. Reingold & 
		     N. Dershowitz (2nd ed., 2001)
		[21] DeltaT webpage by V. Reijs: 
		     http://www.iol.ie/~geniet/eng/DeltaTeval.htm
	1.5 An accurate long-time precession model compatible with P03:
		J. Vondrak, N. Capitaine, P. Wallace: New precession expressions,
		valid for long time intervals.
		Astronomy&Astrophysics 534, A22 (2011); 
		DOI: 10.1051/0004-6361/201117274
	1.6 Nutation: 
		Dennis D. McCarthy and Brian J. Luzum: An Abridged Model of the
		Precession-Nutation of the Celestial Pole.
		Celestial Mechanics and Dynamical Astronomy 85: 37-49, 2003.
		This model provides accuracy better than 1 milli-arcsecond in the
		time 1995-2050. It is applied for years -4000..+8000 only.

2. Included source code
	2.1 Some computation of the sidereal time (sidereal_time.h/c) and pluto
	    orbit contains code from the libnova library (LGPL) by Liam Girdwood.
	2.2 The orbit.cpp/h and solve.h files are directly borrowed from
	    Celestia (Chris Laurel). (GPL license)
	2.3 Several implementations of IMCCE theories for planet and satellite
	    movement by Johannes Gajdosik (MIT-style license,
	    see the corresponding files for the license text)
	2.4 The tesselation algorithms were originally extracted from the glues 
	    library version 1.4 Mike Gorchak <mike@malva.ua> (SGI FREE SOFTWARE
	    LICENSE B).
	2.5 OBJ loader in the Scenery3D plugin based on glObjViewer (c) 2007 
	    dhpoware
	2.6 Parts of the code to work with DE430 and DE431 data files have been 
	    taken from Project Pluto (GPL license).
	2.7 The SpoutLibrary.dll and header from the SpoutSDK version 2.005 
	    available at http://spout.zeal.co (BSD license).
	2.8 The SGP4 satellite propagator in the Satellites plug-in -
	    plugins/Satellites/src/gsatellite/sgp4unit.{cpp,h},
	    sgp4ext.{cpp,h} and sgp4io.{cpp,h} - is the companion code to
	    Vallado D.A., Crawford P., Hujsak R., Kelso T.S., "Revisiting
	    Spacetrack Report #3", AIAA 2006-6753 (2006), by David Vallado,
	    distributed by CelesTrak at
	    https://celestrak.org/publications/AIAA/2006-6753/
	    The copies here are modified. The files carry no copyright notice
	    and no licence text of their own; the author publishes the code
	    for public use. They are third-party work not covered by the
	    grant above.
	2.9 FFmpeg 7.1.3 is linked into the Android package. It is not part of
	    this source tree; it arrives as a dependency of Qt Multimedia and
	    is shipped as libavcodec, libavformat, libavutil, libswresample
	    and libswscale. FFmpeg is copyright its authors and is used here
	    under the GNU Lesser General Public License, version 2.1 or later;
	    COPYING.LGPL21 ships with the application. The binaries are the
	    FFmpeg builds Qt distributes for Android; the complete source code
	    of FFmpeg releases is published at https://ffmpeg.org/releases/
	    and https://ffmpeg.org/
	2.10 Qt 6.10.1 is the application framework the program is built on, and
	    its shared libraries ship inside the Android package. Qt is
	    copyright The Qt Company Ltd. and other contributors, and is used
	    here under the GNU Lesser General Public License, version 3
	    (COPYING.LGPL3 ships with the application). The Qt Charts module
	    is offered under the GNU General Public License, version 3 only,
	    which is why the combined work is delivered under GPL-3 (see the
	    LICENSE block above). Qt's complete source code is published at
	    https://code.qt.io/ and https://download.qt.io/
	2.11 OpenSSL is packaged into the Android APK as libssl and libcrypto
	    so that https:// requests work. The prebuilt Android binaries come
	    from KDAB's android_openssl repository, pinned by commit in
	    src/CMakeLists.txt. OpenSSL is copyright The OpenSSL Project
	    Authors and is used under the Apache License 2.0; COPYING.APACHE2
	    ships with the application. https://www.openssl.org/
	2.12 AndroidX Core 1.13.1 (androidx.core:core) is compiled into the
	    Android package. Copyright The Android Open Source Project, used
	    under the Apache License 2.0 (COPYING.APACHE2).
	    https://developer.android.com/jetpack/androidx
	2.13 md4c 0.5.2, the Markdown parser that renders the sky-culture
	    descriptions, is statically linked into the binary.
	    Copyright (c) 2016-2024 Martin Mitáš. MIT license; the notice at
	    2.19 below applies. https://github.com/mity/md4c
	2.14 QXlsx 1.5.1.1, the spreadsheet writer behind the .xlsx exports,
	    is statically linked into the binary.
	    Copyright 2017-, https://github.com/j2doll/QXlsx. MIT license;
	    the notice at 2.19 below applies.
	2.15 NLopt 2.9.0, the optimisation library used by the Lens Distortion
	    Estimator plug-in, is statically linked into the binary. Copyright
	    its authors; used under the GNU Lesser General Public License,
	    version 2.1 or later, with portions under the MIT license
	    (COPYING.LGPL21). https://github.com/stevengj/nlopt
	2.16 INDI (libindi) 2.1.3, the instrument-control library used by the
	    Telescope Control plug-in, is statically linked into the binary.
	    Copyright its authors; used under the GNU Lesser General Public
	    License, version 2.1 or later (COPYING.LGPL21).
	    https://github.com/indilib/indi
	2.17 src/external/qtcompress, the QZip reader and writer extracted
	    from Qt, is compiled into the binary. Copyright (C) 2013 Digia
	    Plc and/or its subsidiary(-ies); used under the GNU Lesser
	    General Public License, version 2.1 (COPYING.LGPL21).
	2.18 fast_float 8.2.0, a header-only number parser, is compiled into
	    the Android binary because the NDK's C++ library lacks
	    floating-point std::from_chars. Copyright its authors; offered
	    under the Apache License 2.0, the MIT license or the Boost
	    Software License 1.0, and used here under the Apache License 2.0
	    (COPYING.APACHE2). https://github.com/fastfloat/fast_float
	2.19 The MIT license text that 2.13 and 2.14 require to travel with
	    every copy:
	    Permission is hereby granted, free of charge, to any person
	    obtaining a copy of this software and associated documentation
	    files (the "Software"), to deal in the Software without
	    restriction, including without limitation the rights to use,
	    copy, modify, merge, publish, distribute, sublicense, and/or
	    sell copies of the Software, and to permit persons to whom the
	    Software is furnished to do so, subject to the following
	    conditions:
	    The above copyright notice and this permission notice shall be
	    included in all copies or substantial portions of the Software.
	    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
	    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
	    OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
	    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
	    HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
	    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
	    FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
	    OTHER DEALINGS IN THE SOFTWARE.

3. Data
	3.1 The Hipparcos star catalog
	    From ESA (European Space Agency) and the Hipparcos mission.
	    ref. ESA, 1997, The Hipparcos and Tycho Catalogues, ESA SP-1200
	    http://cdsweb.u-strasbg.fr/ftp/cats/I/239
	    The proper motions, parallaxes and parallax errors this fork ships
	    are from the new reduction of the Hipparcos data:
	    ref. van Leeuwen F., 2007, "Validation of the new Hipparcos
	    reduction", A&A 474, 653, catalogue I/311/hip2
	    https://cdsarc.cds.unistra.fr/viz-bin/cat/I/311
	    License: served by the CDS with no restriction on reuse; ESA asks
	    that the mission and the reference be cited.
	3.2 The solar system data mainly comes from IMCCE and partly from
	    Celestia.
	3.3 Polynesian constellations are based on diagrams from the Polynesian
	    Voyaging Society
	3.4 Chinese constellations are based on diagrams from the Hong Kong
	    Space Museum
	3.5 Egyptian constellations are based on the work of Juan Antonio
	    Belmonte, Instituto de Astrofisica de Canarias
	3.6 The Tycho-2 Catalogue of the 2.5 Million Brightest Stars
	    Hog E., Fabricius C., Makarov V.V., Urban S., Corbin T.,
	    Wycoff G., Bastian U., Schwekendiek P., Wicenec A.
	    <Astron. Astrophys. 355, L27 (2000)>
	    http://cdsweb.u-strasbg.fr/ftp/cats/I/259
	3.7 Naval Observatory Merged Astrometric Dataset (NOMAD) version 1
	    http://www.nofs.navy.mil/nomad
	    Norbert Zacharias writes:
	    "There are no fees, both UCAC and NOMAD are freely available
	    with the only requirement that the source of the data (U.S.
	    Naval Observatory) and original product name need to be provided
	    with any distribution, as well as a description about any
	    changes made to the data, if at all."
	    The changes made to the data are:
	    -) try to compute visual magnitude and color from the b,v,r
	       values
	    -) compute nr_of_measurements = the number of valid b,v,r values
	    -) throw away or keep stars (depending on magnitude,
	       nr_of_measurements, combination of flags, tycho2 number)
	    -) add all stars from Hipparcos (incl. component solutions), and
	       tycho2+1st supplement
	    -) reorganize the stars in several brightness levels and
	       triangular zones according to position and magnitude
	    The position, magnitudes, and proper motions of the stars coming
	    from NOMAD are unchanged, except for a possible loss of precision,
	    especially in magnitude. When there is no v-magnitude, it is
	    estimated from r or b magnitude.  When there is no b- or v-
	    magnitude, the color B-V is estimated from the other magnitudes.
	    Also proper motions of faint stars are neglected at all.
	    Those changes are upstream Stellarium's; the programs that made
	    them ("MakeCombinedCatalogue", "ParseHip", "ParseNomad") are in
	    upstream's tree, not this repository. This fork rebuilt the
	    catalogue from Stellarium 24.4's files with a converter written
	    for Asterium, which made these further changes:
	    -) transcodes the pre-26 record format into the current one:
	       zone-relative planar coordinates become barycentric 3D vectors
	    -) carries every star from J2000 to the J2016.0 epoch and rescales
	       parallax for that motion
	    -) replaces the Gaia DR2 parallaxes and parallax errors with the
	       values from the new Hipparcos reduction named in 3.1 above; a
	       star that reduction does not list is shipped with no parallax
	    -) takes proper motions for Hipparcos stars from upstream's
	       hip_pm.dat, the same new reduction
	    -) drops rows keyed by a Gaia source identifier from the auxiliary
	       tables
	    No Gaia data remains in the star catalogue.
	3.8 Stellarium's Catalog of Variable Stars based on General Catalogue of
	    Variable Stars (GCVS) version 5.1, compiled at the Sternberg
	    Astronomical Institute and the Institute of Astronomy of the Russian
	    Academy of Sciences. Free to use with the citation the compilers ask
	    for:
	    Samus' N.N., Kazarovets E.V., Durlevich O.V., Kireeva N.N.,
	    Pastukhova E.N., General Catalogue of Variable Stars: Version GCVS 5.1,
	    Astronomy Reports, 2017, vol. 61, No. 1, pp. 80-88
	    http://www.sai.msu.su/gcvs/gcvs/
	    http://cdsarc.u-strasbg.fr/viz-bin/Cat?cat=B%2Fgcvs&
	3.9 Consolidated DSO catalog was created from various data:
	    [1]  NGC/IC data taken from SIMBAD Astronomical Database
	         http://simbad.u-strasbg.fr
	    [2]  Distance to NGC/IC data taken from NED (NASA/IPAC EXTRAGALACTIC
	         DATABASE) 
	         http://ned.ipac.caltech.edu
	    [3]  Catalogue of HII Regions (Sharpless, 1959) taken from VizieR
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/20
	    [4]  H-α emission regions in Southern Milky Way (Rodgers+, 1960)
	         taken from VizieR
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/216
	    [5]  Catalogue of Reflection Nebulae (Van den Bergh, 1966) taken
	         from VizieR
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/21
	    [6]  Lynds' Catalogue of Dark Nebulae (LDN) (Lynds, 1962) taken
	         from VizieR
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/7A
	    [7]  Lynds' Catalogue of Bright Nebulae (Lynds, 1965) taken from
	         VizieR
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/9
	    [8]  Catalog of bright diffuse Galactic nebulae (Cederblad, 1946)
	         taken from VizieR
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/231
	    [9]  Barnard's Catalogue of 349 Dark Objects in the Sky (Barnard,
	         1927) taken from VizieR
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/220A
	    [10] A Catalogue of Star Clusters shown on Franklin-Adams Chart
	         Plates (Melotte, 1915) taken from NASA ADS
	         http://adsabs.harvard.edu/abs/1915MmRAS..60..175M
	    [11] On Structural Properties of Open Galactic Clusters and their
	         Spatial Distribution. Catalog of Open Galactic Clusters.
	         (Collinder, 1931) taken from NASA ADS
	         http://adsabs.harvard.edu/abs/1931AnLun...2....1C
	    [12] The Collinder Catalog of Open Star Clusters. An Observer’s
	         Checklist.
	         Edited by Thomas Watson. Taken from CloudyNights
	         http://www.cloudynights.com/page/articles/cat/articles/the-collinder-catalog-updated-r2467
	    [13] OpenNGC, the open NGC/IC database by Mattia Verga
	         DOI:10.21938/y.1ejWUD_MQ6b_eDFoVbbw
	         https://github.com/mattiaverga/OpenNGC
	         License: Creative Commons Attribution-Share Alike 4.0
	         International https://creativecommons.org/licenses/by-sa/4.0/
	         The consolidated catalogue is a derivative of it and is under
	         that licence.
	3.10 Cross-identification of objects in consolidated DSO catalog was
	     made with:
	    [1]  SIMBAD Astronomical Database
	         http://simbad.u-strasbg.fr
	    [2]  Merged catalogue of reflection nebulae (Magakian, 2003)
	         http://vizier.u-strasbg.fr/viz-bin/VizieR-3?-source=J/A+A/399/141
	    [3]  Messier Catalogue was taken from Wikipedia (includes
	         morphological classification and distances)
	         https://en.wikipedia.org/wiki/List_of_Messier_objects
	    [4]  Caldwell Catalogue was taken from Wikipedia (includes
	         morphological classification and distances)
	         https://en.wikipedia.org/wiki/Caldwell_catalogue
	    [5]  Atlas of Peculiar Galaxies (Arp, 1966)
	         https://ned.ipac.caltech.edu/level5/Arp/frames.html
	    [6]  Interacting galaxies catalogue (Vorontsov-Velyaminov+, 2001)
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/236
	    [7]  Catalogue of Galactic Planetary Nebulae (Kohoutek, 2001)
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=IV/24
	    [8]  Strasbourg-ESO Catalogue of Galactic Planetary Nebulae
	         (Acker+, 1992)
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=V/84
	    [9]  A catalogue of Galactic supernova remnants (Green, 2014)
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/272
	    [10] A catalog of rich clusters of galaxies (Abell+, 1989)
	         http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=VII/110A
	3.11 Morphological classification and magnitudes (partially) for Melotte
	     catalogue was taken from DeepSkyPedia
	     http://deepskypedia.com/wiki/List:Melotte
	3.12 Distances and other parameters in the consolidated DSO catalog were
	     taken from:
	    [1]  The Magellanic Cloud Calibration of the Galactic Planetary Nebula
	         Distance Scale (Stanghellini+, 2008), The Astrophysical Journal,
	         Volume 689, Issue 1, pp. 194-202
	         http://adsabs.harvard.edu/abs/2008ApJ...689..194S
	    [2]  A 1.4-GHz Arecibo Survey for Pulsars in Globular Clusters (2007)
	         https://arxiv.org/abs/0707.1602
	    [3]  Catalog of Parameters for Milky Way Globular Clusters: The
	         Database (Harris, 2010)
	         http://physwww.physics.mcmaster.ca/~harris/mwgc.dat
	    [4]  Distance measurements of Lynds Nebulae (Hilton+, 1995)
	         https://ui.adsabs.harvard.edu/abs/1995A%26AS..113..325H/abstract
	    [5]  The Cygnus X region. V. Catalogue and distances of optically
	         visible H II regions (Dickel+, 1969)
	         https://ui.adsabs.harvard.edu/abs/1969A%26A.....1..270D/abstract
	3.13 The city list in data/base_locations.bin.gz and the country codes in
	     data/iso3166.tab come from GeoNames.
	     http://www.geonames.org/
	     http://download.geonames.org/export/dump/countryInfo.txt
	     License: Creative Commons Attribution 4.0 International
	     https://creativecommons.org/licenses/by/4.0/
	3.14 Double and multiple star data come from the U.S. Naval Observatory:
	     stars/hip_gaia3/wds_hip_part.dat from the Washington Double Star
	     Catalog (WDS), stars/hip_gaia3/binary_orbitparam.dat from the Sixth
	     Catalog of Orbits of Visual Binary Stars (ORB6). Both are works of
	     the United States government and carry no copyright; the USNO asks
	     to be credited as the source.
	     https://www.usno.navy.mil/USNO/astrometry/optical-IR-prod/wds
	     Mason B.D., Wycoff G.L., Hartkopf W.I., Douglass G.G., Worley C.E.,
	     The Washington Double Star Catalog, AJ 122, 3466 (2001)
	3.15 The star cross-identification table stars/hip_gaia3/cross-id.cat is
	     the result of a query against the SIMBAD Astronomical Database,
	     operated at CDS, Strasbourg, France, made with the generator at
	     https://github.com/henrysky/stellarium_star_catalogs (GPL-3.0; the
	     code itself is not part of this work).
	     http://simbad.u-strasbg.fr
	     CDS asks that use of SIMBAD be acknowledged with:
	     Wenger M. et al., The SIMBAD astronomical database, A&AS 143, 9 (2000)
	3.16 The proper and scientific names of stars in
	     stars/hip_gaia3/name.fab and stars/hip_gaia3/extra_name.fab.
	     name.fab holds Bayer and Flamsteed designations, arranged by IAU
	     constellation. extra_name.fab holds double-star discoverer
	     designations from the Washington Double Star Catalog credited in
	     3.14 above, and carries that catalogue's credit. Both tables are
	     upstream Stellarium's compilation, keyed to Hipparcos numbers,
	     and are shipped as upstream made them except that rows keyed by
	     a Gaia source identifier have been removed (see 3.7).
	3.17 The catalogues that the plug-ins carry are compiled into the
	     binary. Their sources are:
	     -) Pulsars: The ATNF Pulsar Catalogue v2.8.1
	        Manchester R.N., Hobbs G.B., Teoh A., Hobbs M.,
	        Astron. J. 129, 1993 (2005)
	        https://www.atnf.csiro.au/research/pulsar/psrcat/
	        The ATNF asks that the catalogue and that paper be cited.
	     -) Exoplanets: The Extrasolar Planets Encyclopaedia
	        https://exoplanet.eu/
	        License: Creative Commons Attribution 4.0 International
	        https://creativecommons.org/licenses/by/4.0/
	        The bundled exoplanets.json is a snapshot of that database.
	     -) Quasars: "Quasars and Active Galactic Nuclei" (13th Ed.),
	        Veron-Cetty M.-P., Veron P., A&A 518, A10 (2010)
	     -) Satellites: orbital elements from CelesTrak (Dr. T.S. Kelso)
	        https://celestrak.org/ and from SatNOGS
	        https://db.satnogs.org/, which publishes under the Creative
	        Commons Attribution-ShareAlike 4.0 International licence
	        https://creativecommons.org/licenses/by-sa/4.0/ ; the bundled
	        satellites.json is in part a derivative of the SatNOGS
	        database and is under that licence.
	        The communications frequencies in communications.json are
	        compiled from the sources each entry names inside the file:
	        chiefly JE9PEL's satellite frequency list
	        https://www.ne.jp/asahi/hamradio/je9pel/satslist.htm, the
	        SatNOGS transmitter database, r4uab.ru and zarya.info.
	        The standard magnitudes and radar cross sections in
	        satellites.dat come from Mike McCants (https://mmccants.org,
	        used with his permission), the MMT-9 observatory
	        http://mmt.favor2.info/satellites and CelesTrak's satellite
	        catalogue, as the file's own header records.
	     -) Comet designations and discovery circumstances in
	        plugins/SolarSystemEditor/resources/comet_discovery.fab:
	        Gary W. Kronk, "Cometography: A Catalog of Comets", vols. 1-6;
	        the Minor Planet Center's periodic comet numbers
	        https://minorplanetcenter.net/iau/lists/PeriodicCodes.html ;
	        the Central Bureau for Astronomical Telegrams; NASA JPL's
	        Small-Body Database; and Seiichi Yoshida's comet catalogue
	        http://www.aerith.net/comet/catalog/index-periodic.html
	     -) Asteroid discovery circumstances in
	        plugins/SolarSystemEditor/resources/discovery_circumstances.dat
	        are from the Minor Planet Center's Numbered Minor Planets file
	        https://minorplanetcenter.net/
	     -) Novae, historical supernovae and meteor shower data
	        (novae.json, supernovae.json, MeteorShowers.json), the camera
	        footprints of the Mosaic Camera plug-in and the telescope list
	        in device_models.json are upstream Stellarium's and the
	        plug-in authors' own compilations of published records, and
	        are covered by the licence grant at the head of this file.
	     -) Minor planet and comet orbital elements are fetched at run time
	        from the Minor Planet Center https://minorplanetcenter.net/
	        and are not shipped inside the application.
	3.18 Reference tables in data/:
	     -) data/nomenclature.dat - the names of surface features on Solar
	        system bodies, from the Gazetteer of Planetary Nomenclature of
	        the IAU Working Group for Planetary System Nomenclature, run by
	        the USGS Astrogeology Science Center
	        https://planetarynames.wr.usgs.gov/
	        A work of the United States government; no copyright. The USGS
	        asks to be credited as the source.
	     -) data/constellations_spans.dat - the IAU constellation
	        boundaries, drawn by Eugene Delporte for the IAU in 1930 and
	        tabulated for epoch B1875 as used here.
	        ref. Roman N.G., "Identification of a Constellation from a
	        Position", PASP 99, 695 (1987), catalogue VI/42
	        https://cdsarc.cds.unistra.fr/viz-bin/cat/VI/42
	     -) data/languages.tab - ISO 639 language codes and names. The codes
	        are an ISO standard and the table is a listing of them.
	     -) data/regions-geoscheme.tab - the United Nations geoscheme
	        (Standard Country or Area Codes for Statistical Use, M49),
	        extended in this table with codes for other Solar system bodies.
	        https://unstats.un.org/unsd/methodology/m49/
	     -) data/asteroid_elements.json - osculating orbital elements
	        generated by util/fetch_asteroid_elements.py from NASA JPL's
	        Horizons system.
	        https://ssd.jpl.nasa.gov/horizons/
	        A work of NASA/JPL-Caltech; no copyright asserted.
	     -) data/base_locations.bin.gz and data/iso3166.tab are GeoNames and
	        are credited at 3.13 above.
	3.19 stars/hip_gaia3/stars_hip_sp_0v0_4.cat, the MK spectral types
	     shown for Hipparcos stars, and stars/hip_gaia3/object_types_v0_1.cat,
	     the table of object-type codes, are upstream Stellarium's own
	     compilations, taken unchanged from Stellarium 24.4 and covered by
	     the licence grant at the head of this file.
	3.20 nebulae/default/discovery.dat, the discoverer and discovery year
	     shown for deep-sky objects, is compiled from the SEDS Messier
	     database and the historical records its own header names.
	     nebulae/default/outlines.dat, the nebula outline polylines, is
	     carried unchanged from upstream Stellarium. Both are covered by
	     the licence grant at the head of this file.


4. Graphics
	All graphics are copyrighted by the Stellarium's Team (GNU GPLv2 or later) 
	except the ones mentioned below :
	The deep-sky photographs listed here are shipped as sky textures: each one
	has been resized, cropped and reprojected onto the tile it fills. Images
	marked ShareAlike stay under their own licence.
	4.1 The "earthmap" texture was created by NASA (Reto Stockli, NASA Earth
	    Observatory) using data from the MODIS instrument aboard the
	    Terra satellite (Public Domain). See chapter 10.1 for full
	    credits.
	4.2 Moon albedo map was generated by Ruslan Kabatsayev from Hapke-normalized
	    LROC WAC data for visible-light wavelengths, plus empirically-normalized
	    data for 643 nm to cover the polar areas. The spectra were converted to
	    linear sRGB, then scaled up by a factor of 4, and then the sRGB inverse
	    transfer function was applied. The polar areas, being monochromatic in the
	    original data set, were colored using a fit to the equatorial sectors. After
	    this the image was color-balanced to yield the grayish look of the texture
	    instead of the natural beige color of the lunar surface.
	    The map is under the Creative Commons Attribution-ShareAlike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	    license.
	4.3a Jupiter map is the Cassini cylindrical map of Jupiter, built from
	     narrow-angle camera images taken during the spacecraft's flyby of
	     the planet. NASA/JPL Photojournal PIA07782, "Cassini Best Maps of
	     Jupiter Cylindrical Map":
	     https://assets.science.nasa.gov/content/dam/science/psd/photojournal/pia/pia07/pia07782/PIA07782.jpg
	     Credit: NASA/JPL/Space Science Institute. Cropped from 3601x1801 to
	     3600x1800 and downsampled to 512x256 for this fork.
	     License: public domain. NASA does not claim copyright, and asks
	     only that its material be credited and that no NASA endorsement
	     be implied.
	     https://www.nasa.gov/nasa-brand-center/images-and-media/
	4.3b The Iapetus map and the rings of Uranus and Neptune
	     are from Celestia (http://shatters.net/celestia/)
	     under the GNU General Purpose License, version 2 or any later
	     version:
	     - Iapetus map is from dr. Fridger Schrempp (t00fri).
	4.3c The Amalthea, Europa, Ganymede, Gaspra,Mercury, Uranus, Neptune, Mimas,
	     Bianca, Epimetheus, Ida, Vesta, Hyperion, Io, Janus, Phobos, Phoebe, Prometheus,
	     Saturn, Tethys, Venus, Ceres, Uranus, Epimetheus, Deimos, Proteus and comet
             maps are processed by Oleg Pluton a.k.a Helleformer
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.3d The Titania, Umbriel, Pluto, Charon, Sedna and 2007 OR10 maps are created
	     by Kexitt and postprocessed by Oleg Pluton a.k.a Helleformer
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.3e The Ariel, Haumea, Miranda, Oberon and Nereid maps are created by 
	     Snowfall and postprocessed by Oleg Pluton a.k.a Helleformer
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.3f The Eris and Dysnomia maps are created by MrSpace43 and postprocessed by 
	     Oleg Pluton a.k.a Helleformer
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.3g The Rhea map are created by FarGetaNik and postprocessed by Oleg Pluton 
	     a.k.a Helleformer
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.3i The Titan (Clouds) map are created by Magenta Meteorite and postprocessed
	     by Oleg Pluton a.k.a Helleformer
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.3k Callisto map is the Voyager - Galileo SSI global mosaic from the USGS
	     Astrogeology Science Center
	     https://astrogeology.usgs.gov/search/map/callisto_galileo_voyager_simple_cylindrical_global_map
	     downsampled and colored for this fork. License: public domain.
	4.3l Dione and Enceladus maps are created by NASA (CICLOPS team) 
	     from Cassini data, colored by RVS. Public domain.
	4.3m All other planet maps from David Seal's site:
	     http://maps.jpl.nasa.gov/   see license in section 10.2
	4.3n Bennu map is created by NASA from OSIRIS-REx spacecraft data. Public domain.
	     Full size mosaic: https://www.asteroidmission.org/bennu_global_mosaic/
	4.3o The Sun map was created by Ruslan Kabatsayev from the images taken by SDO HMI.
	     The map is under the Creative Commons Attribution-ShareAlike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	     license.
	4.3p textures/zodiacallight_2004.png is a rendering of a published
	     brightness model, made for Stellarium by Georg Zotti. The texture is
	     the Stellarium team's own work and is covered by the grant at the
	     head of this section.
	     ref. Kwon S.M., Hong S.S., Weinberg J.L., "An observational model of
	     the zodiacal light brightness distribution", New Astronomy 10, 91
	     (2004). doi:10.1016/j.newast.2004.05.004
	     ref. Leinert C., "Zodiacal Light - A Measure of the Interplanetary
	     Environment", Space Science Reviews 18, 281 (1975)
	4.3q textures/eros.png and the observer-viewpoint images
	     textures/obs_earth.jpg, obs_mars.jpg, obs_jupiter.jpg,
	     obs_saturn.jpg, obs_uranus.jpg, obs_neptune.jpg and obs_sun.jpg
	     are carried unchanged from upstream Stellarium and are covered
	     by the licence grant at the head of this section.
	4.4 The fullsky milky way panorama is taken from "Deep Star Maps 2020"
	    by Ernie Wright (USRA), NASA/Goddard Space Flight Center
	    Scientific Visualization Studio, visualization 4851:
	    https://svs.gsfc.nasa.gov/4851
	    The panorama is the celestial-coordinate Milky Way background
	    layer, built from the faint (magnitude 8 to 21) stars of Gaia
	    Data Release 2. Credit: NASA/Goddard Space Flight Center
	    Scientific Visualization Studio. Gaia DR2: ESA/Gaia/DPAC.
	    License: public domain. NASA does not claim copyright, and asks
	    only that its material be credited and that no NASA endorsement
	    be implied.
	    https://www.nasa.gov/nasa-brand-center/images-and-media/
	4.6 M31 pictures come from LEE ang HG731GZ
	    License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.7 Images of NGC4526, NGC6544, NGC6553
	    from Yang Kai
	    License: public domain 
	4.10 Constellation art, GUI buttons, logo created by Johan Meuris
	     (Jomejome) (jomejome at users.sourceforge.net)
	     http://www.johanmeuris.eu/
	     License: released under the Free Art License
	     (http://artlibre.org/licence.php/lalgb.html)
	     Icon created by Johan Meuris
	     License: Creative Commons Attribution-ShareAlike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.11 The "earth-clouds" texture includes imagery owned by NASA.
	     See NASA's Visible Earth project at http://visibleearth.nasa.gov/
	     License: 1. The imagery is free of licensing fees
		      2. NASA requires that they be provided a credit as the 
		         owners of the imagery
	     The cloud texturing was taken from Celestia (GPL),
	     http://www.shatters.net/celestia/
	4.12 The folder icon derived from the Tango Desktop Project, used under
	     the terms of the Creative Commons Attribution Share-Alike
	     license.
	4.13 Images of NGC7317, NGC7319, NGC7320
	     from Andrey Kuznetsov, Kepler Observatory
	     http://kepler-observatorium.ru
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.14 Images of NGC2903, NGC3185, NGC3187, NGC3189,
	     NGC3190, NGC3193, NGC3718, NGC3729, NGC5981, NGC5982,
	     NGC5985
	     from Oleg Bryzgalov 
	     http://olegbr.astroclub.kiev.ua/
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.15 Image of eta Carinae 
	     from Harel Boren
	     http://www.pbase.com/
	     License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.16 Images of NGC3726
	     from KPNO/NOIRLab/NSF/AURA/George Hickey/Adam Block, post-processing: Sun Shuwei
	     https://noirlab.edu/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.17 Images of NGC3324
	     from Trevor Gerdes
	     http://www.sarcasmogerdes.com/
	4.19 Images of NGC3293
	     from ESO/G. Beccari
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.20 Images of SMC (Magellanic Clouds)
	     from ESA/Hubble and Digitized Sky Survey 2
	     https://commons.wikimedia.org/wiki/File:Small_Magellanic_Cloud_(Digitized_Sky_Survey_2).jpg
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.21 Images of NGC1261, NGC3201, NGC4833, NGC5286, NGC5823, NGC6025, NGC6087,
	     NGC6101, NGC6124, NGC6352, NGC6397, NGC6541, NGC6752
	     from Tian Mai, Image processing: Sun Shuwei
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.22 The Vesta and Ceres map was taken from USGS website
	     https://astrogeology.usgs.gov/
	     and colored by RVS. License: public domain.
	4.23 Images of IC1727, NGC467, NGC470, NGC2814, NGC2820, NGC3512, NGC4657,
	     PGC1803573
	     from Peter Vasey, Plover Hill Observatory
	     http://www.madpc.co.uk/~peterv/
	4.24 Image of IC3568 from Howard Bond (Space Telescope Science Institute), Robin Ciardullo (Pennsylvania State University) and NASA
	     License: public domain
	4.25 Image of solar corona from eclipse 2008-08-01 by Georg Zotti
	4.27 Images of NGC2261, NGC2818, NGC2936, NGC3314, NGC3690, NGC3918,
	     NGC4038-4039, NGC5257, NGC5307, NGC6027, NGC6050, NGC6369, NGC6826, NGC7742, IC883,
	     IC4406, PGC2248, UGC1810, UGC8335, UGC9618, Red Rectangle and Calabash Nebula
	     from NASA, ESA, the Hubble Heritage (STScI/AURA)-ESA/Hubble
	     Collaboration, and K. Noll (STScI)
	     License: public domain; http://hubblesite.org/copyright/
	4.29 Images of NGC2467, NGC6590, Barnard Loop, IC342
	     from Sun Shuwei
	     License: public domain
	4.30 Images of M77, NGC3180, NGC5474, NGC6231, Sh2-264, Sh2-308, LDN1622
	     from Wang Lingyi
	     License: public domain
	4.31 Images of PGC6830, PGC29653 from Lowell Observatory
	     http://www2.lowell.edu/
	     License: public domain
	4.32 Images of M89, IC2631, PGC10074, Virgo Cluster
	     from ESO/Digitized Sky Survey 2
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.32a Images of IC2220, NGC1433, NGC3572, RCW32
	     from ESO/Digitized Sky Survey 2. Acknowledgement: Davide De Martin
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.32b Images of RCW38
	     from ESO/Digitized Sky Survey 2. Acknowledgment: Davide De Martin
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.32c Images of NGC2434, Fornax Cluster
	     from ESO and Digitized Sky Survey 2. Acknowledgment: Davide De Martin
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.32d Images of RCW49
	     from ESO (Wide Field Imager image of the Gum 29 region, of which
	     RCW 49 is the nebula)
	     https://www.eso.org/public/news/eso0837/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.33 Images of NGC3603 from ESO/La Silla Observatory
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.34 Images of NGC4228, NGC4244
	     from Ole Nielsen
	     License: Creative Commons Attribution-Share Alike 2.5 Generic https://creativecommons.org/licenses/by-sa/2.5/ 
	4.35 Images of NGC2808, NGC7023, Hercules Cluster from NASA
	     License: public domain
	4.36 Images of M44, IC1396 from Giuseppe Donatiello
	     License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.37 Images of M12, M14, M20, M22, M55, M56, M62, M88, M92, M108, 
	     IC5146, NGC225, NGC281, NGC663, NGC891, NGC6823, NGC7814
	     from Hewholooks
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.39 Images of M61, M64, M65, M66, M91, M99, M100, NGC613, NGC772, NGC918, NGC1042, NGC1360, NGC1398,
	     NGC1501, NGC1535, NGC1555, NGC2158, NGC2282, NGC2346, NGC2362, NGC2440, NGC2683, NGC2775, NGC3132,
	     NGC3242, NGC3486, NGC3750, NGC4170, NGC4216, NGC4361, NGC4395, NGC4414,,NGC4450, NGC4676, NGC4725,
	     NGC5216, NGC5248, NGC5426, NGC5466, NGC6302, NGC6522, NGC6543, NGC6563, NGC6781, NGC6894, NGC7000,
	     NGC7354, NGC7822, UGC3697, Abell31, Abell33, Abell39, Cassiopeia A, LBN438, Wild's Triplet
	     from Adam Block/Mount Lemmon SkyCenter/University of Arizona
	     http://www.caelumobservatory.com/
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.40 Images of IC1295, NGC134, NGC92, NGC55, NGC908, NGC936, NGC1232, NGC1313,
	     NGC1792, NGC1978, NGC2207, NGC2736, NGC3199, NGC3699, NGC3766, NGC3532, NGC4945, NGC5128, NGC5189,
	     NGC6537, NGC6769, NGC6822, NGC7793
	     from ESO
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
 	4.41 Images of M33, M42, NGC253, NGC5566, LBN782, the Pleiades from HG731GZ
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.42 Images of M8, M17, IC2944, IC4628, NGC5367, NGC6357,
	     NGC6334, NGC7293, LDN43, SMC (Hydrogen Alpha)
	     from Dylan O'Donnell
	     http://deography.com/
	     License: public domain
	4.44 Images of Sh2-73, Sh2-129
	     from Maurizio Cabibbo, post-processing: Sun Shuwei
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.45 Images of M16, IC4592, IC4601, NGC4236, NGC6914, Perseus Cluster
	     from Sun Gang
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.46 Images of NGC4651
	     from R. Jay GaBany
	     https://www.cosmotography.com/images/
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.47 Images of NGC7662
	     from Gianluca.pollastri
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.48 Images of M2, M3, M4, M5, M13, M10, M28, M30, M53, M69, M71, M75, M85, M94,
	     M102, M107, IC10, NGC6144, PGC143, UGC12613
	     from Starhopper
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.49 Images of M95, NGC520, NGC660, NGC925, NGC1300, NGC4490, NGC6888, NGC7129, NGC7497
	     from Jschulman555
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.50 Images of M98
	     from Clh288
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 2.5 Generic https://creativecommons.org/licenses/by-sa/2.5/
	4.51 Images of NGC6818
	     from Robert Rubin (NASA/ESA Ames Research Center), Reginald Dufour and Matt Browning (Rice University), Patrick Harrington (University of Maryland), and NASA/ESA
	     https://www.spacetelescope.org/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.52 Images of NGC6188
	     from Ivan Bok
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.53 Images of NGC6188
	     from Friendlystar
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.53a Images of Barnard 72 (the Snake Nebula)
	     from Friendlystar
	     https://commons.wikimedia.org/wiki/File:Snake_Nebula.jpg
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.54 Images of NGC1023
	     from Fryns Fryns
	     https://commons.wikimedia.org/wiki/
	     License:  public domain
	4.55 Images of NGC3195
	     from Zhu Ying
	     License:  Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.56 Images of IC2118, NGC1269, NGC1499
	     from Zhao Jingna
	     License:  Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.57 Images of UGC10822
	     from Science NASA, ESA, Eduardo Vitral (STScI), Roeland van der Marel (STScI), Sangmo Tony Sohn (STScI), DSS Image Processing: Joseph DePasquale (STScI)
	     https://science.nasa.gov/asset/hubble/draco-dwarf-spheroidal/
	     License: Public domain
	4.58 Images of IC2169, NGC2244, NGC3077, SH2-155, SH2-263, Barnard 22, Barnard207,
	     Abell85
	     from Keesscherer
	     https://commons.wikimedia.org/wiki/
	     License:  Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.59 Images of NGC3115
	     from Mi Lan
	     License:  Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.60 Images of NGC3621
	     from ESO and Joe DePasquale
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.61 Images of NGC2547
	     from ESO/J.Perez
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.62 Images of M83
	     from TRAPPIST/E. Jehin/ESO
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.63 Images of DWB111, IC405, IC410, IC443, IC1805, IC1848, IC2177,
	     NGC288, NGC404, NGC1097, NGC1931, NGC2264, LBN437, Sh2-191, Sh2-247
	     from Giuseppe Donatiello, post-processing: Sun Shuwei
	     https://flickr.com/photos/133259498@N05/
	     License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.64 Images of NGC1055
	     from Jeffjnet
	     http://www.iceinspace.com.au/
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.65 Images of NGC2867, NGC6884
	     from Howard Bond (ST ScI) and NASA/ESA
	     https://www.spacetelescope.org/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.66 Images of NGC4567
	     from Klaus Hohmann
	     https://commons.wikimedia.org/wiki
	     License: Creative Commons Attribution-Share Alike 3.0 Germany https://creativecommons.org/licenses/by-sa/3.0/de/
	4.67 Images of RCW53
	     from Kong Fanxi
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.68 Images of NGC5897
	     from San Esteban
	     http://www.astrosurf.com
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.69 Images of NGC6388
	     from ESO, F. Ferraro (University of Bologna)
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.70 Images of NGC7380, SH2-80, SH2-106
	     from Cristina Cellini
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.71 Images of NGC6744
	     from Zhuokai Liu, Jiang Yuhang
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.72 Images of NGC2170, SH2-240
	     from Rogelio Bernal Andreo
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.73 Images of VDB 152
	     from Hubble Space Telescope
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.74 Images of NGC6905
	     from Tom Wildoner
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.75 Images of NGC2623
	     from NASA, ESA and A. Evans (Stony Brook University, New York, University of Virginia & National Radio Astronomy Observatory, Charlottesville, USA)
	     https://www.spacetelescope.org/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.76 Images of IC4634, NGC3808, NGC5882, NGC6210, NGC6572, NGC6741
	     from ESA/Hubble & NASA
	     https://www.spacetelescope.org/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.77 Images of NGC6240, PGC33423
	     from NASA, ESA, the Hubble Heritage Team (STScI/AURA)-ESA/Hubble Collaboration and A. Evans (University of Virginia, Charlottesville/NRAO/Stony Brook University), K. Noll (STScI), and J. Westphal (Caltech)
	     https://www.spacetelescope.org/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.78 Images of UGC5470
	     from Friendlystar
	     https://ko.wikipedia.org/
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.79 Images of UGC6253, UGC9749
	     from Giuseppe Donatiello
	     https://en.wikipedia.org/
	     License: public domain
	4.80 Images of UGC10214
	     from NASA, Holland Ford (JHU), the ACS Science Team and ESA
	     https://www.spacetelescope.org/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.81 Images of NGC7714
	     from NASA, ESA, Digitized Sky Survey 2
	     https://esahubble.org/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.81a Images of NGC6503
	     from NASA, ESA, Digitized Sky Survey 2 (Acknowledgement: Davide De Martin)
	     https://esahubble.org/images/heic1513b/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.81b Images of Hydra Cluster
	     from NASA, ESA, Digitized Sky Survey 2 (Acknowledgement: Davide de Martin)
	     https://esahubble.org/images/heic1208b/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.82 Images of NGC2477
	     from Guillermo Abramson
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.83 Images of NGC4755
	     from ESO/Y. Beletsky
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.84 Images of NGC7538
	     from J. Aleu
	     https://commons.wikimedia.org/wiki/
	     License: public domain
	4.85 Images of IC2395, IC4756, NGC3114, NGC6633
	     from Roberto Mura
	     https://en.wikipedia.org/
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.86 Images of NGC362
	     from ESO/VISTA VMC
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.87 Images of NGC2997
	     from Adam Block/ChileScope
	     http://www.caelumobservatory.com/
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.88 Images of M74, NGC1788, NGC5053, Sh2-132, Coma Cluster, Leo Cluster, barnard150, LDN673
	     from Bart Delsaert
	     https://delsaert.com/
	     License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.89 Images of NGC1333
	     from AAE-Agrupacio Astronomica d'Eivissa
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.90 Images of NGC5139
	     from Jose Mtanous
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.91 Images of NGC6960
	     from Jose Mtanous
	     https://commons.wikimedia.org/wiki/
	     License: public domain
	4.92 Images of NGC6445
	     from The Pan-STARRS1 Surveys (PS1) and the PS1 public science archive have been made possible through contributions by the Institute for Astronomy, the University of Hawaii, the Pan-STARRS Project Office, the Max-Planck Society and its participating institutes, the Max Planck Institute for Astronomy, Heidelberg and the Max Planck Institute for Extraterrestrial Physics, Garching, The Johns Hopkins University, Durham University, the University of Edinburgh, the Queen's University Belfast, the Harvard-Smithsonian Center for Astrophysics, the Las Cumbres Observatory Global Telescope Network Incorporated, the National Central University of Taiwan, the Space Telescope Science Institute, the National Aeronautics and Space Administration under Grant No. NNX08AR22G issued through the Planetary Science Division of the NASA Science Mission Directorate, the National Science Foundation Grant No. AST-1238877, the University of Maryland, Eotvos Lorand University (ELTE), the Los Alamos National Laboratory, and the Gordon and Betty Moore Foundation.
	     https://ps1images.stsci.edu/cgi-bin/ps1cutouts
	     License: public domain
	4.93 Images of LMC (Magellanic Clouds)
	     from ESO/R. Gendler and Sun Shuwei
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/. This work is a derivative of "Map of the Large Magellanic Cloud" and "The entire Large Magellanic Cloud with annotations" by ESO/R. Gendler, used under Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/. This work is licensed under Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ by Sun Shuwei.
	4.94 Images of rho Oph
	     from ESO/S. Guisard
	     http://eso.org/public/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.95 Images of IC59, IC63
	     from Zhang Ruiping
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.96 Images of NGC6366
	     from Robert Eder, post-processing: Sun Shuwei
	     https://commons.wikimedia.org/wiki/
	     License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.97 Images of IC418, IC4593, NGC5315, NGC6751
	     from NASA/ESA and The Hubble Heritage Team (STScI/AURA)
	     https://www.spacetelescope.org/
	     License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.98 Images of NGC7009 from ESO/J. Walsh
	      http://eso.org/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.99 Images of NGC7027 from Judy Schmidt
	      https://commons.wikimedia.org/wiki/
	      License: public domain 
	4.100 Images of NGC6309, NGC6891 from Fabian RRRR
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.101 Images of NGC2022 from ESA/Hubble & NASA, R. Wade
	      https://www.spacetelescope.org/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.102 Images of NGC654 from Antonio F. Sanchez
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.103 Images of NGC6496
	      from Mohamad Abbas
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.104 Images of PGC3589
	      from ESA/Hubble, Digitized Sky Survey 2
	      https://www.spacetelescope.org/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.105 Images of NGC1672
	      from Davide De Martin (ESA/Hubble), the ESA/ESO/NASA Photoshop FITS Liberator & Digitized Sky Survey 2
	      https://www.spacetelescope.org/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.106 Images of NGC4298
	      from NASA, ESA, Digitized Sky Survey 2; Acknowledgement: Davide De Martin
	      https://www.spacetelescope.org/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.107 Images of NGC1502
	      from Kamil Pecinovsky
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.108 Images of NGC752
	      from Tayson82
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.109 Images of Sh2-170
	      from Wang Wei
	      License: Creative Commons Attribution-Share Alike 3.0 Unported https://creativecommons.org/licenses/by-sa/3.0/
	4.110 Images of NGC2613
	      from ESO/IDA/Danish 1.5 m/R. Gendler, J.-E. Ovaldsen, C. Thone and C. Feron
	      http://eso.org/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.111 The miscWorldMap.jpg image was created from NASA Blue Marble Next Generation
	      dataset for July 2004. Public domain.
	      https://visibleearth.nasa.gov/images/74092/july-blue-marble-next-generation
	4.112 Images of M80, M90, RCW101
	      from NOIRLab/NSF/AURA, post-processing: Sun Shuwei 
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.113 Images of Dark Doodad Nebula, IC1613, NGC1216, NGC4372
	      from Hansjorg Walchli, post-processing: Sun Shuwei
	      https://www.deepskycorner.ch/index.de.php
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.114 Images of NGC1566, NGC1808
	      from Dark Energy Survey/DOE/FNAL/DECam/CTIO/NOIRLab/NSF/AURA, Image processing: T.A. Rector (University of Alaska Anchorage/NSF NOIRLab), J. Miller (Gemini Observatory/NSF NOIRLab), M. Zamani & D. de Martin (NSF NOIRLab), post-processing: Sun Shuwei
	      https://www.deepskycorner.ch/index.de.php
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.115 Images of IC289, NGC185
	      from Thedarksideobservatory, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.116 Images of NGC5005
	      from KPNO/NOIRLab/NSF/AURA/Ray and Emily Magnani/Adam Block
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.117 Images of NGC80, NGC147, NGC1491, NGC1624, NGC7026
	      from Radek Chromik, post-processing: Sun Shuwei
	      https://www.deepskycorner.ch/index.de.php
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.118 Images of IC4954
	      from KPNO/NOIRLab/NSF/AURA/Adam Block, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.119 Images of IC2574, Sh2-313
	      from Jerry Macon, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.120 Images of NGC5395
	      from KPNO/NOIRLab/NSF/AURA/Doug Matthews and E. J. Jones/Adam Block, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.121 Images of RCW77 (the Engraved Hourglass Nebula, MyCn 18)
	      from NASA, R. Sahai, J. Trauger (JPL), and The WFPC2 Science Team
	      https://esahubble.org/images/opo9607a/
	      License: public domain
	4.122 Images of Sh2-216
	      from Ram samudrala, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.123 Images of Sh2-174, Sh2-239
	      from T.A. Rector (University of Alaska Anchorage) and H. Schweiker (WIYN and NOIRLab/NSF/AURA), post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.124 Images of Sh2-82, Sh2-91, Sh2-119, Sh2-135, Sh2-235,  Sh2-261, VDB158
	      from Sebastian Goralik, post-processing: Sun Shuwei
	      https://flickr.com/photos/sebastiangoralik/
	      License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.126 Images of NGC3109
	      from Legacy Surveys / D. Lang (Perimeter Institute), DESI Legacy Imaging Surveys; additional processing: Giuseppe Donatiello, Sun Shuwei
	      https://flickr.com/photos/133259498@N05/
	      License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.127 Images of Sh2-157
	      from Sebastian Goralikpost-processing: Sun Shuwei
	      https://flickr.com/photos/sebastiangoralik/
	      License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.128 Images of Sh2-1
	      from Nicolarge, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/ 
	4.129 Images of RCW86
	      from CTIO/NOIRLab/NSF/AURA/T.A. Rector (University of Alaska Anchorage/NSF NOIRLab) Image processing: T.A. Rector (University of Alaska Anchorage/NSF NOIRLab), M. Zamani (NSF NOIRLab) & D. de Martin (NSF NOIRLab), post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License:  Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.130 Images of NGC7318
	      from Juan lacruz, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/ 
	4.131 Images of IC1505, NGC70, NGC529, NGC1530, NGC2366, NGC2768, NGC3227, NGC3561,
	      NGC6621, NGC6951, NGC7139, NGC7741, UGC4305, Jones1
	      from Juan lacruz, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.132 Images of M78, IC348, IC423, IC434, NGC6726, LBN550, LDN1251, Sh2-124, RCW29,
	      RCW58, RCW89, VDB123, Vela supernova remnant
	      from Manuel Peitsch, post-processing: Sun Shuwei
	      https://manuel-astro.ch/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.133 Images of M7, NGC1316, NGC2442, NGC2899, RCW100, Sh2-54, Westerlund1
	      from ESO, post-processing: Sun Shuwei
	      http://eso.org/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.134 Images of NGC2210, NGC2579
	      from Legacy Surveys / D. Lang (Perimeter Institute); additional processing: Meli thev, Sun Shuwei
	      https://www.legacysurvey.org/acknowledgment
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.135 Images of NGC3628, Sh2-113
	      from Chuck Ayoub, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.136 Images of M49, M59, M60, NGC547, NGC2681, NGC2976, NGC3198, NGC3626, NGC3893,
	      NGC4096, NGC4147, NGC4494, NGC4517, NGC4527, NGC4664, NGC4697, NGC4753,
	      NGC4762, NGC5068, NGC762, NGC6229, NGC6535, NGC6934, NGC7006, NGC7619,
	      UGC9792
	      from Sloan Digital Sky Survey
	      https://live-sdss4org-dr14.pantheonsite.io/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/ 
	4.137 Images of NGC3211, NGC7492
	      from Legacy Surveys / D. Lang (Perimeter Institute); additional processing: Meli thev, Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.138 Images of Frosty Leo Nebula
	      from ESA/Hubble & NASA, post-processing: Sun Shuwei
	      http://www.spacetelescope.org/
	      License: public domain
	4.139 Images of NGC4666
	      from ESO/J. Dietrich, post-processing: Sun Shuwei
	      http://www.eso.org/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.140 Images of M104
	      from Adam Block/Mount Lemmon SkyCenter/University of Arizona & Ngc1535, post-processing, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.141 Images of M72
	      from Starhopper, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.142 Images of NGC4699
	      from KPNO/NOIRLab/NSF/AURA/Michael Vogel and Robert Mitsch/Adam Block, Image processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.143 Images of NGC6872
	      from Abdallah jawhari, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.144 Images of UGC5373
	      from KPNO/NOIRLab/NSF/AURA Data obtained and processed by: P. Massey (Lowell Obs.), G. Jacoby, K. Olsen, & C. Smith (AURA/NSF) Image processing: T.A. Rector (University of Alaska Anchorage/NSF NOIRLab), M. Zamani (NSF NOIRLab) & D. de Martin (NSF NOIRLab), post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.145 Images of M54, M70
	      from REU Program/NOIRLab/NSF/AURA, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.146 Images of sh2-71
	      from Cristina Cellini, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.147 Images of DWB20, sh2-173
	      from Astrophoto Andy, post-processing: Sun Shuwei
	      https://www.flickr.com/photos/andyweeks/53246079280/in/dateposted/
	      License: public domain
	4.148 Images of MHC6325, NGC6355, NGC6440, NGC6638, NGC6642, NGC6717, NGC6749
	      from The Pan-STARRS1 Surveys (PS1) and the PS1 public science archive have been made possible through contributions by the Institute for Astronomy, the University of Hawaii, the Pan-STARRS Project Office, the Max-Planck Society and its participating institutes, the Max Planck Institute for Astronomy, Heidelberg and the Max Planck Institute for Extraterrestrial Physics, Garching, The Johns Hopkins University, Durham University, the University of Edinburgh, the Queen's University Belfast, the Harvard-Smithsonian Center for Astrophysics, the Las Cumbres Observatory Global Telescope Network Incorporated, the National Central University of Taiwan, the Space Telescope Science Institute, the National Aeronautics and Space Administration under Grant No. NNX08AR22G issued through the Planetary Science Division of the NASA Science Mission Directorate, the National Science Foundation Grant No. AST-1238877, the University of Maryland, Eotvos Lorand University (ELTE), the Los Alamos National Laboratory, and the Gordon and Betty Moore Foundation, Image processing: Sun Shuwei
	      https://ps1images.stsci.edu/cgi-bin
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/ 
	4.149 Images of NGC5946,NGC6517
	      from Donald Pelletier, post-processing: Sun Shuwei 
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-ShareAlike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/ 
	4.150 Images of WR134
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Luc Viatour / https://Lucnix.be at https://commons.wikimedia.org/wiki/File:WR134-Hamois-06-08-2024-Luc-Viatour.jpg.
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.151 Images of NGC3503
	      from Donald Cappellettiariel, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.152 Images of IC239, NGC5850
	      from Adam Block/Mount Lemmon SkyCenter/University of Arizona, Image processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.153 Images of NGC1569
	      from Adam Block/ Josep Drudis/Mount Lemmon SkyCenter/University of Arizona, Ngc1535, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.154 Images of NGC45, NGC6337
	      from Meli thev, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.155 Images of UGC11668
	      from KPNO/NOIRLab/NSF/AURA/Chas Sourek and Diana Hartrampf/Adam Block, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.156 Images of PGC3074547
	      from NASA, ESA and The Hubble Heritage Team STScI/AURA, post-processing: Sun Shuwei
	      https://esahubble.org/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.157 Images of NGC1365
	      from ESO/IDA/Danish 1.5 m/ R. Gendler, J-E. Ovaldsen, C. Th?ne, and C. Feron., post-processing: Sun Shuwei
	      http://www.eso.org/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.158 Images of NGC289
	      from Adam Block/ChileScope, Ngc1535, post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.159 Images of NGC6565
	      from ESA/Hubble & NASA, Acknowledgement: M. Novak, post-processing: Sun Shuwei
	      https://esahubble.org/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.160 Images of M109
	      from KPNO/NOIRLab/NSF/AURA/George Hatfield and Flynn Haase, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.161 Images of IC5076, NGC2419, NGC4157, Ced211
	      from Adam Block/Mount Lemmon SkyCenter/University of Arizona, post-processing: Sun Shuwei
	      http://www.caelumobservatory.com/
	      License: Creative Commons Attribution-Share Alike 3.0 United States https://creativecommons.org/licenses/by-sa/3.0/us/
	4.162 Images of Necklace Nebula
	      from NASA, ESA and the Hubble Heritage Team (STScI/AURA), post-processing: Sun Shuwei
	      https://esahubble.org/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.163 Images of NGC2300
	      from NASA, ESA, STScI, Adam Block (Steward Observatory), post-processing: Sun Shuwei
	      https://commons.wikimedia.org/wiki/File:NGC_2276_Wide-Field_(2021-029).png
	      License: public domain
	4.164 Images of M19
	      from Doug Williams, REU Program/NOIRLab/NSF/AURA, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.165 Images of IC1276
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Yu-Hang Kuo at https://www.flickr.com/photos/143529236@N06/51139472108/in/dateposted/.
	      https://www.flickr.com/
	      License: Creative Commons Attribution-ShareAlike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/ 
	4.166 Images of NGC5371
	      from KPNO/NOIRLab/NSF/AURA/Joe Jordan/Adam Block, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.167 Images of NGC3953
	      from KPNO/NOIRLab/NSF/AURA/Tom and Gail Haynes/Adam Block, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.169 Images of M81, NGC1579
	      from Kees Scherer, post-processing: Sun Shuwei
	      https://www.flickr.com/photos/kees-scherer/
	      License: public domain
	4.170 Images of NGC7635, Sh2-136, VDB 14, VDB 15
	      from K Bahr, post-processing: Sun Shuwei
	      https://www.flickr.com/photos/158350039@N03/
	      License: public domain
	4.171 Images of NGC3786
	      from Rudy Kokich, post-processing: Sun Shuwei
	      https://www.flickr.com/photos/140097441@N02/
	      License: public domain
	4.172 Images of NGC1851
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Yu-Hang Kuo at https://www.flickr.com/photos/143529236@N06/46234214731/.
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.173 Images of NGC2808
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Yu-Hang Kuo at https://www.flickr.com/photos/143529236@N06/31856303507/.
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.174 Images of NGC6356
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Yu-Hang Kuo at https://www.flickr.com/photos/143529236@N06/48912086568/.
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.175 Images of NGC6362
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Yu-Hang Kuo at https://www.flickr.com/photos/143529236@N06/43257251884/.
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.176 Images of NGC6760
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Yu-Hang Kuo at https://www.flickr.com/photos/143529236@N06/51137814465/.
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.177 Images of Sh2-112
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Carsten Frenzl at https://flickr.com/photos/castro-pic/50667636156/.
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.178 Images of Sh2-115
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Carsten Frenzl at https://flickr.com/photos/castro-pic/50666893358/.
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.179 Images of NGC2985
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Carsten Frenzl at https://flickr.com/photos/191225735@N03/51941601154.
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.180 Images of M9
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Yu-Hang Kuo at https://www.flickr.com/photos/143529236@N06/48912773222/.
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.181 Images of M68
	      This image file was remixed, transformed, and post-processed by Sun Shuwei from the original file. The original image file was published by Yu-Hang Kuo at https://www.flickr.com/photos/143529236@N06/47278554542/.
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.182 Images of Sh2-280
	      from Wang Jianjun, post-processing: Sun Shuwei
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.183 Images of Sh2-282, Sh2-284
	      from ESO/Digitized Sky Survey 2. Acknowledgement: Davide De Martin, post-processing: Sun Shuwei
	      http://eso.org/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.184 Images of NGC7048
	      from KPNO/NOIRLab/NSF/AURA/Richard Robinson and Beverly Erdman/Adam Block, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.185 Images of NGC4449
	      from KPNO/NOIRLab/NSF/AURA/John and Christie Connors/Adam Block, post-processing: Sun Shuwei
	      https://noirlab.edu/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.186 Images of NGC3640
	      from ESO/INAF/M. Mirabile et al./R. Ragusa et al, post-processing: Sun Shuwei
	      http://www.eso.org/public/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.187 Images of NGC5102, NGC6293, NGC6304, NGC6316, NGC6441, NGC6569, NGC6652,
	      NGC6712
	      from the Two Micron All Sky Survey (2MASS), a joint project of the University of Massachusetts and the Infrared Processing and Analysis Center/California Institute of Technology, funded by the National Aeronautics and Space Administration and the National Science Foundation. Credit line requested by IPAC: "Atlas Image courtesy of 2MASS/UMass/IPAC-Caltech/NASA/NSF"
	      https://www.ipac.caltech.edu/page/image-use-policy
	      License: public domain. Images on IPAC public web sites may be used for any purpose without prior permission; the 2MASS Atlas Images are in the public domain. No image may be used to suggest NASA, JPL or Caltech endorsement of a commercial product
	4.188 Images of IC1287, IC2162, Jones-Emberson 1, M1, M101, M103, M105, M106, M11, M15,
	      M18, M21, M23, M25, M26, M27, M29, M34, M35, M36, M37, M38, M39,
	      M41, M46, M47, M48, M50, M51, M52, M57, M58, M63, M67, M73, M76,
	      M79, M82, M93, M96, M97, Medusa, NGC1407, NGC1514, NGC1560,
	      NGC1961, NGC2146, NGC2174, NGC2359, NGC2371, NGC2392, NGC2403,
	      NGC246, NGC247, NGC2506, NGC2655, NGC2685, NGC2805, NGC2841,
	      NGC3079, NGC3166, NGC3310, NGC3344, NGC3359, NGC3504, NGC3521,
	      NGC3923, NGC3938, NGC40, NGC4151, NGC4394, NGC4535, NGC4559,
	      NGC4565, NGC457, NGC4631, NGC4636, NGC4656, NGC474, NGC488,
	      NGC5033, NGC5363, NGC5634, NGC5694, NGC5906, NGC6235, NGC6284,
	      NGC6287, NGC6342, NGC6401, NGC6539, NGC672, NGC691, NGC6946,
	      NGC7008, NGC7331, NGC7479, NGC7640, NGC7789, Sh2-101, Sh2-140,
	      Sh2-188, Sh2-301
	      from a Pan-STARRS1 3pi DR2 grizy composite built for Asterium. The Pan-STARRS1 Surveys (PS1) and the PS1 public science archive have been made possible through contributions by the Institute for Astronomy, the University of Hawaii, the Pan-STARRS Project Office, the Max-Planck Society and its participating institutes, the Max Planck Institute for Astronomy, Heidelberg and the Max Planck Institute for Extraterrestrial Physics, Garching, The Johns Hopkins University, Durham University, the University of Edinburgh, the Queen's University Belfast, the Harvard-Smithsonian Center for Astrophysics, the Las Cumbres Observatory Global Telescope Network Incorporated, the National Central University of Taiwan, the Space Telescope Science Institute, the National Aeronautics and Space Administration under Grant No. NNX08AR22G issued through the Planetary Science Division of the NASA Science Mission Directorate, the National Science Foundation Grant No. AST-1238877, the University of Maryland, Eotvos Lorand University (ELTE), the Los Alamos National Laboratory, and the Gordon and Betty Moore Foundation.
	      https://archive.stsci.edu/publishing/data-use
	      License: public domain under MAST's data use policy, which asks that the mission and STScI be acknowledged. The composite itself is Asterium's, under GNU GPLv2 or later with the rest of the work
	4.189 Images of IC4499
	      from ESA/Hubble & NASA
	      https://commons.wikimedia.org/wiki/File:Potw1431a.jpg
	      License: Creative Commons Attribution 3.0 Unported https://creativecommons.org/licenses/by/3.0/
	4.190 Images of NGC7424
	      from ESO
	      https://commons.wikimedia.org/wiki/File:Magnificent_Spiral_Galaxy_NGC_7424.jpg
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.191 Images of RCW98
	      from Legacy Surveys / D. Lang (Perimeter Institute); additional processing: Meli thev
	      https://commons.wikimedia.org/wiki/File:RCW_98_DECaPS_DR2.jpg
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.192 Images of NGC2298
	      from NASA, STScI, WikiSky
	      https://commons.wikimedia.org/wiki/File:NGC_2298.jpg
	      License: public domain
	4.193 Images of PGC54392
	      from NASA, Sloan Digital Sky Survey
	      https://commons.wikimedia.org/wiki/File:PGC_54392_(SDSS_II).jpg
	      License: public domain
	4.194 Images of M24
	      from the Two Micron All Sky Survey (2MASS), UMass/IPAC-Caltech/NASA/NSF
	      https://commons.wikimedia.org/wiki/File:Messier_024_2MASS.jpg
	      License: public domain
	4.195 Images of NGC4274
	      from Chuck Ayoub
	      https://commons.wikimedia.org/wiki/File:NGC_4274_Group.png
	      License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.196 Images of Barnard 142
	      from Chuck Ayoub
	      https://commons.wikimedia.org/wiki/File:ENebula.png
	      License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.197 Images of NGC300, NGC6868
	      from the DESI Legacy Imaging Surveys, DR9 for NGC300 and DR10 for NGC6868. Credit: Legacy Surveys / D. Lang (Perimeter Institute)
	      https://www.legacysurvey.org/acknowledgment/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.198 Images of NGC5927, NGC6139, NGC6624
	      from DECaPS2, the second data release of the DECam Plane Survey. Credit: Legacy Surveys / D. Lang (Perimeter Institute)
	      https://www.legacysurvey.org/acknowledgment/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.199 Images of NGC104 (47 Tucanae)
	      from ESO/M.-R. Cioni/VISTA Magellanic Cloud survey. Acknowledgment: Cambridge Astronomical Survey Unit
	      https://commons.wikimedia.org/wiki/File:New_VISTA_snap_of_star_cluster_47_Tucanae.jpg
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.200 Images of NGC121
	      from the DESI Legacy Imaging Surveys DR10. Credit: Legacy Surveys / D. Lang (Perimeter Institute)
	      https://www.legacysurvey.org/acknowledgment/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.201 Images of NGC5824
	      from the DESI Legacy Imaging Surveys DR10, r band rendered as greyscale. Credit: Legacy Surveys / D. Lang (Perimeter Institute)
	      https://www.legacysurvey.org/acknowledgment/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.202 Images of NGC4976, NGC5986, NGC6584
	      from the unwise-neo7 layer of the Legacy Surveys viewer, the co-added WISE and NEOWISE imaging, rendered as greyscale. Credit, in the wording the surveys require: unWISE / NASA/JPL-Caltech / D. Lang (Perimeter Institute)
	      https://www.legacysurvey.org/acknowledgment/
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.203 Images of M6
	      from N.A.Sharp, Mark Hanna, REU program/NOIRLab/NSF/AURA
	      https://commons.wikimedia.org/wiki/File:M6,_NGC_6405;_Butterfly_Cluster_(noao-02637).jpg
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.204 Images of NGC1532
	      from ESO/IDA/Danish 1.5 m/R.Gendler and J.-E. Ovaldsen
	      https://commons.wikimedia.org/wiki/File:ESO_-_Ngc1532_gendler_(by).jpg
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.205 Images of NGC7590
	      from Legacy Surveys / D. Lang (Perimeter Institute); additional processing: Meli thev
	      https://commons.wikimedia.org/wiki/File:NGC_7590_NGC_7599_legacy_dr10.jpg
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.206 Images of RCW158
	      from ESO/VPHAS+ team
	      https://commons.wikimedia.org/wiki/File:There_is_an_impostor_in_this_nebula_(potw2518a).jpg
	      License: Creative Commons Attribution 4.0 International https://creativecommons.org/licenses/by/4.0/
	4.207 Images of NGC1549
	      from Roberto Mura
	      https://commons.wikimedia.org/wiki/File:NGC1549-1553.jpg
	      License: public domain
	4.208 Images of PGC47847
	      from NASA, SDSS
	      https://commons.wikimedia.org/wiki/File:PGC_47847_(SDSS_II).jpg
	      License: public domain
	4.209 Images of Sadr region (Gamma Cygni)
	      from Giuseppe Donatiello
	      https://commons.wikimedia.org/wiki/File:Gamma_Cygni_Combine10s_(18842750261).jpg
	      License: Creative Commons CC0 1.0 Universal Public Domain Dedication https://creativecommons.org/publicdomain/zero/1.0/
	4.210 Images of LDN1235
	      from Stephen Andrew Kennedy of Strawn, Texas
	      https://commons.wikimedia.org/wiki/File:LDN_1235,_popularly_known_as_the_Shark_Nebula.jpg
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.211 Images of NGC869, NGC884
	      from Markus Pössel
	      https://commons.wikimedia.org/wiki/File:H_and_%CF%87_Persei.jpg
	      License: Creative Commons Attribution-Share Alike 4.0 International https://creativecommons.org/licenses/by-sa/4.0/
	4.212 The Moon landscape (landscapes/moon/apollo17.png) is NASA's
	      photograph from the Apollo 17 landing site, and the Mars landscape
	      (landscapes/mars/mars.png) is the "Husband Hill Summit" 360-degree
	      panorama combined from images taken by the panoramic camera on
	      NASA's Mars Exploration Rover Spirit on sols 583 to 586
	      (24-27 August 2005), assembled by Michael Smith.
	      Credit: NASA for the Moon, NASA/JPL-Caltech/Cornell for Mars.
	      https://www.nasa.gov/nasa-brand-center/images-and-media/
	      License: public domain. NASA does not claim copyright, and asks
	      only that its material be credited and that no NASA endorsement be
	      implied.
	4.213 The remaining landscapes are the Stellarium team's own work and are
	      covered by the blanket grant at the head of this section:
	      garching and guereins by Fabien Chéreau; geneva by Georg
	      Zotti, horizon definition by Patrick Chevalley; hurricane by Johan
	      Meuris and Robert Spearman; jupiter, neptune and uranus by Alexander
	      Wolf; ocean by Michael Smith; saturn, sun and zero by Georg Zotti;
	      trees by Robert Spearman, light pollution image by Georg Zotti.
	      License: GNU General Public License, version 2 or later.
	4.214 The fifteen small-body shape models in models/ come from NASA's
	      Planetary Data System. Ten are from the Stooke Small Body Shape
	      Models V2.0 data set — 1682q1halley, 243ida, 951gaspra, j5amalthea,
	      n7larissa, n8proteus, s10janus, s11epimetheus, s16prometheus and
	      s17pandora — and five from the Small Body Shape Models V2.1 data
	      set, the optical shape models of P. Thomas: 4vesta_21,
	      951gaspra_21, s7hyperion_21, m1phobos and m2deimos.
	      Stooke, P., Stooke Small Body Shape Models V2.0.
	      EAR-A-5-DDR-STOOKE-SHAPE-MODELS-V2.0. NASA Planetary Data System,
	      2016.
	      Thomas, P.C., Small Body Shape Models V2.1.
	      EAR-A-5-DDR-SHAPE-MODELS-V2.1. NASA Planetary Data System.
	      https://sbnarchive.psi.edu/pds3/non_mission/
	      License: public domain. PDS data carry no copyright; the archive
	      asks that the data set and its author be cited.
	      The texture each model carries (models/*.png) is the same body's
	      map credited at 4.3b, 4.3c or 4.22, rotated 180 degrees in
	      longitude to fit the models' UV layout.
	4.215 The Moon mesh (models/moon-vertices-indices.bin.7z, 1.5 million
	      vertices) and the Moon's normal and horizon maps
	      (textures/moon_normals.png, textures/moon_horizon.png) are all
	      derived from the elevation data of NASA's CGI Moon Kit, which is
	      built from Lunar Reconnaissance Orbiter Laser Altimeter
	      measurements. The normal and horizon maps were produced from that
	      elevation model with map-bumper.
	      https://svs.gsfc.nasa.gov/4720
	      License: public domain. Credit requested: "NASA's Scientific
	      Visualization Studio".

5. Fonts
	5.1 NotoSans-Regular.ttf (version 2.007, Copyright 2015-2021 Google LLC),
	    NotoSansMono-Regular.ttf (version 2.010, Copyright 2022 The Noto
	    Project Authors, https://github.com/notofonts/latin-greek-cyrillic)
	    and NotoSansSC-Regular.otf (version 2.002, Copyright 2014-2020 Adobe,
	    http://www.adobe.com/; Noto is a trademark of Google Inc.)
	    License: SIL Open Font License, version 1.1; the full text ships in
	    data/OFL.txt beside the fonts.
	    https://openfontlicense.org
	5.2 DejaVuSans.ttf and DejaVuSansMono.ttf, from the DejaVu Fonts project,
	    which extends Bitstream Vera. Both carry the Bitstream Vera Fonts
	    Copyright and the DejaVu changes notice inside their own name tables.
	    https://dejavu-fonts.github.io/
```

## Appendix
```
1 Full credits for image 4.1
	Author: Reto Stockli, NASA Earth Observatory,
		rstockli (at) climate.gsfc.nasa.gov
	Address of correspondence:
		Reto Stockli
		ETH/IAC (NFS Klima) & NASA/GSFC Code 913 (SSAI)
		University Irchel
		Building 25 Room J53
		Winterthurerstrasse 190
		8057 Zurich, Switzerland
	Phone:  +41 (0)1 635 5209
	Fax:    +41 (0)1 362 5197
	Email:  rstockli (at) climate.gsfc.nasa.gov
	http://earthobservatory.nasa.gov
	http://www.iac.ethz.ch/staff/stockli
	Supervisors:
		Fritz Hasler and David Herring, NASA/Goddard Space Flight Center
	Funding:
		This project was realized under the SSAI subcontract 2101-01-027
		(NAS5-01070)

	License :
		"Any and all materials published on the Earth Observatory are
		freely available for re-publication or re-use, except where
		copyright is indicated."

2 License for the JPL planets images
(http://www.jpl.nasa.gov/images/policy/index.cfm)

    Unless otherwise noted, images and video on JPL public web sites (public
    sites ending with a jpl.nasa.gov address) may be used for any purpose
    without prior permission, subject to the special cases noted below.
    Publishers who wish to have authorization may print this page and retain
    it for their records; JPL does not issue image permissions on an image
    by image basis.  By electing to download the material from this web site
    the user agrees:
    1. that Caltech makes no representations or warranties with respect to
       ownership of copyrights in the images, and does not represent others
       who may claim to be authors or owners of copyright of any of the
       images, and makes no warranties as to the quality of the images.
       Caltech shall not be responsible for any loss or expenses resulting
       from the use of the images, and you release and hold Caltech harmless
       from all liability arising from such use.
    2. to use a credit line in connection with images. Unless otherwise
       noted in the caption information for an image, the credit line should
       be "Courtesy NASA/JPL-Caltech."
    3. that the endorsement of any product or service by Caltech, JPL or
       NASA must not be claimed or implied.
    Special Cases:
    * Prior written approval must be obtained to use the NASA insignia logo
      (the blue "meatball" insignia), the NASA logotype (the red "worm"
      logo) and the NASA seal. These images may not be used by persons who
      are not NASA employees or on products (including Web pages) that are
      not NASA sponsored. In addition, no image may be used to explicitly
      or implicitly suggest endorsement by NASA, JPL or Caltech of
      commercial goods or services. Requests to use NASA logos may be
      directed to Bert Ulrich, Public Services Division, NASA Headquarters,
      Code POS, Washington, DC 20546, telephone (202) 358-1713, fax (202)
      358-4331, email bert.ulrich@hq.nasa.gov.
    * Prior written approval must be obtained to use the JPL logo (stylized
      JPL letters in red or other colors). Requests to use the JPL logo may
      be directed to the Television/Imaging Team Leader, Media Relations
      Office, Mail Stop 186-120, Jet Propulsion Laboratory, Pasadena CA
      91109, telephone (818) 354-5011, fax (818) 354-4537.
    * If an image includes an identifiable person, using the image for
      commercial purposes may infringe that person's right of privacy or
      publicity, and permission should be obtained from the person. NASA
      and JPL generally do not permit likenesses of current employees to
      appear on commercial products. For more information, consult the NASA
      and JPL points of contact listed above.
    * JPL/Caltech contractors and vendors who wish to use JPL images in
      advertising or public relation materials should direct requests to the
      Television/Imaging Team Leader, Media Relations Office, Mail Stop
      186-120, Jet Propulsion Laboratory, Pasadena CA 91109, telephone
      (818) 354-5011, fax (818) 354-4537.
    * Some image and video materials on JPL public web sites are owned by
      organizations other than JPL or NASA. These owners have agreed to
      make their images and video available for journalistic, educational
      and personal uses, but restrictions are placed on commercial uses.
      To obtain permission for commercial use, contact the copyright owner
      listed in each image caption.  Ownership of images and video by
      parties other than JPL and NASA is noted in the caption material
      with each image.
```
