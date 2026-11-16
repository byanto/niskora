# Boundary file for the province map

Research for issue #31. Done on **2026-11-16**. The candidates were downloaded and simplified in a scratch folder outside the repo with mapshaper 0.6.102. Every size and count below was measured on those downloads. No data file is committed with this note.

The question: which boundary file of Indonesia's 38 provinces should the location analysis map use? The map is plain SVG in the React app. It is drawn from a file committed to the repo and projected with `d3-geo`, about 600 px wide.

## 1. Summary and recommendation

**Use Badan Informasi Geospasial (BIG), the government mapping agency.** Take its national administrative boundary service `BATASWILAYAH/BATAS_KABKOTA_AR` (the June 2026 edition of the 1:50,000 regency and city boundaries). Dissolve the 541 regency polygons into 38 provinces on the Kemendagri province code `KDPPUM`. Simplify, and commit one TopoJSON file of about **75 KB** (23 KB gzipped).

Why:

- It is the **only primary source found with all 38 provinces**. geoBoundaries, OCHA HDX COD-AB and Natural Earth stop at 33 or 34 (section 2).
- Each polygon carries the **Kemendagri code** (`KDPPUM`, for example `93` for Papua Selatan). The 38 codes and names match the Kepmendagri 300.2.2-2430 Tahun 2025 list exactly, including "Daerah Khusus Ibukota Jakarta". So the join key is the official two-digit code that niskora already matches destinations against (section 4).
- It is the boundary that Indonesian law defines as official. PP 45/2021 puts provincial boundaries in the national base map (Peta Rupabumi Indonesia), drawn by BIG from the Kemendagri regulations (section 3).
- Licence: there is no Creative Commons style licence. UU 4/2011 makes this basic geospatial data **open to anyone** (Pasal 42). UU 28/2014 says that publishing, distributing or copying works the government has published is not copyright infringement unless they are marked as protected (Pasal 43 huruf b). BIG's service carries no such mark. The one restriction found is that a derivative product **made for sale** needs the owner's permission (UU 4/2011 Pasal 50). niskora does not sell the map file, so committing it to a public repo is allowed. Credit the source anyway: "Sumber: Badan Informasi Geospasial (BIG), batas wilayah administrasi 1:50.000, edisi Juni 2026" (section 3).

Steps to produce the committed file are in section 5.

**Small islands.** At 600 px wide one pixel is about 8.5 km, roughly 70 km² of land. After simplifying, 176 of the 188 island and mainland polygons of 100 km² or more survive, and 145 of the 401 between 10 and 100 km². Everything under 10 km² drops out; at this width those would be smaller than a pixel (section 5).

## 2. Candidates compared

| Source | Provinces | Format | Province key in properties | Licence | Verdict |
|---|---|---|---|---|---|
| **BIG** `BATAS_KABKOTA_AR` (edition June 2026) | **38** (541 regency polygons, dissolve to provinces) | ArcGIS REST, query returns GeoJSON | `KDPPUM` = Kemendagri code; `WADMPR` = name; `KDPBPS` (BPS code) empty | Open by law (UU 4/2011 Pasal 42); attribution good practice; no sale of derivatives without permission (Pasal 50) | **Recommended** |
| BIG `BATAS_WILAYAH` layer 12 (2022-2024 editions) | 38 (539 regency polygons) | same | `kdppum`, `wadmpr` (lower case); Jakarta named "DKI Jakarta" | same | Older copy of the same data |
| geoBoundaries gbOpen IDN ADM1 | 34 | GeoJSON, TopoJSON | `shapeName` only; `shapeISO` empty | ODbL 1.0 (from OSM, year represented 2017) | Rejected: 34, share-alike |
| geoBoundaries gbHumanitarian IDN ADM1 | 34 | GeoJSON, TopoJSON | `shapeName` ("Dki Jakarta"); `shapeISO` empty | CC BY 3.0 IGO (from OCHA) | Rejected: 34 |
| OCHA HDX COD-AB Indonesia | 34 ("Admin 1: 34 province") | GDB, SHP, GeoJSON (456 MB zip), XLSX | P-codes `ID11`...; source BPS | CC BY-IGO | Rejected: 34. Boundaries are from 2020 (reviewed in October 2025, not updated) |
| Natural Earth 10m admin 1 | **33** (no Kalimantan Utara) | GeoJSON, SHP | `iso_3166_2` (`ID-JK`...), names like "Jakarta Raya", "Bangka-Belitung", "Yogyakarta" | Public domain | Rejected: 33 |
| GADM | n/a (not downloaded) | GPKG, SHP, GeoJSON | n/a | "Redistribution or commercial use is not allowed without prior permission." | Rejected: licence forbids committing it |
| OpenStreetMap extract | Not counted live (Overpass servers refused queries) | any | Varies by extract | ODbL: "If you alter or build upon our data, you may distribute the result only under the same license." | Rejected: a committed simplified extract is a derived database, so the file would have to be published under ODbL with OSM credit |
| Community repo `denyherianto/indonesia-geojson-topojson-maps-with-38-provinces` | 38 | GeoJSON 254 KB, TopoJSON 49 KB | `KODE_PROV` (Kemendagri style, e.g. `32`), `PROVINSI` | Data stated as CC BY 4.0, but its `DATA_LICENSE.md` points to a `SOURCE.md` that does not exist, and the article names no upstream source | Rejected: provenance unknown, so the CC BY grant cannot be trusted |

Sources for the table:

- BIG REST services: [BATAS_KABKOTA_AR](https://geoservices.big.go.id/rbi/rest/services/BATASWILAYAH/BATAS_KABKOTA_AR/MapServer/0) (service description "Geodatabase data batas wilayah administrasi nasional edisi Juni 2026"; 541 records; `METADATA` values `TASWIL50000` + dates 2022-12-27 to 2026-06-12) and [BATAS_WILAYAH layer 12](https://geoservices.big.go.id/rbi/rest/services/BATASWILAYAH/BATAS_WILAYAH/MapServer/12) (539 records, metadata dates up to 2024-09-05). Both list `geoJSON` among the supported query formats and have `maxRecordCount` 1000.
- geoBoundaries API: [gbOpen](https://www.geoboundaries.org/api/current/gbOpen/IDN/ADM1/) and [gbHumanitarian](https://www.geoboundaries.org/api/current/gbHumanitarian/IDN/ADM1/) (`admUnitCount` 34 in both, licence fields quoted above).
- HDX: [CKAN API for cod-ab-idn](https://data.humdata.org/api/3/action/package_show?id=cod-ab-idn) (licence `cc-by-igo`, notes "Admin 1: 34 province", source BPS, boundaries "valid for use" from 1 April 2020).
- Natural Earth: [ne_10m_admin_1_states_provinces.geojson](https://github.com/nvkelso/natural-earth-vector/blob/master/geojson/ne_10m_admin_1_states_provinces.geojson), 33 features with `adm0_a3` = `IDN`.
- GADM: [licence page](https://gadm.org/license.html).
- OSM: [copyright page](https://www.openstreetmap.org/copyright).
- Community repo: [GitHub](https://github.com/denyherianto/indonesia-geojson-topojson-maps-with-38-provinces) (`DATA_LICENSE.md`, `metadata.json`) and the [author's article](https://dev.to/denyherianto/geojson-topojson-maps-of-indonesia-38-provinces-591n).

The BPS geoportal (`geoportal.bps.go.id`) was also tried. It did not answer from this network, so it was not assessed.

## 3. Licence of the BIG data

BIG's web service shows no licence text and its `copyrightText` field is empty. The Ina-Geoportal page shows no terms of use either. Its status comes from Indonesian law:

- **Provincial boundaries are part of the basic geospatial data (IGD).** PP 45/2021 Pasal 6 ayat (1) lists "batas wilayah" as an element of the base map, and ayat (2) says the base map is the Peta Rupabumi Indonesia. Pasal 10 ayat (3) splits administrative boundaries into province, regency/city, district and village. Pasal 10 ayat (4) says they are drawn "berdasarkan dokumen yang mengikat secara hukum" - for provinces and regencies these are Kemendagri regulations, according to the explanation of the regulation. Pasal 10 ayat (5) says that where no binding document exists yet, a provisional boundary is drawn with a distinct symbol or colour. ([PP 45/2021 body](https://peraturan.go.id/files/pp45-2021bt.pdf), [explanation](https://peraturan.go.id/files/pp45-2021pjl.pdf))
- **IGD is open.** UU 4/2011 Pasal 42: "IGD ... bersifat terbuka". The explanation: "bersifat terbuka" means "IGD dapat diakses dan dapat diperoleh oleh setiap orang" (anyone can access and obtain it). ([UU 4/2011](https://peraturan.go.id/files/uu4-2011.pdf))
- **Selling derivatives needs permission.** UU 4/2011 Pasal 50: anyone "yang membuat produk turunan suatu IG dengan maksud untuk diperjualbelikan wajib mendapat izin dari pemilik IG". A simplified province map inside an internal business tool, committed to a repo, is not made to be sold. If niskora is ever sold as a product, ask BIG for permission or check this again.
- **The Cipta Kerja law left these articles alone.** UU 11/2020 Pasal 20 changes UU 4/2011 Pasal 1, 7, 13, 17, 18, 28 and 55, deletes Pasal 12 and 56, and inserts Pasal 22A. It does not touch Pasal 42 or 50. ([UU 11/2020 Bab III Bagian Ketiga on Wikisource](https://id.wikisource.org/wiki/Undang-Undang_Republik_Indonesia_Nomor_11_Tahun_2020/Bab_III/Bagian_Ketiga))
- **Copyright.** UU 28/2014 Pasal 43 huruf b: publishing, distributing or copying "segala sesuatu yang dilaksanakan oleh atau atas nama pemerintah" does not infringe copyright, "kecuali dinyatakan dilindungi oleh peraturan perundang-undangan, pernyataan pada Ciptaan tersebut ...". BIG's service carries no such statement. ([UU 28/2014](https://peraturan.go.id/files/uu28-2014bt.pdf))

**Attribution.** No rule found makes credit mandatory for this use, but it is cheap and honest. Put it in the file's folder (a short `SOURCE.md` or README line next to the TopoJSON) and as a small caption under the map: "Batas provinsi: Badan Informasi Geospasial, 1:50.000, edisi Juni 2026, disederhanakan."

**Residual risk.** This is a reading of the statutes, not a licence grant from BIG, and there is no explicit open licence to point at. The risk is low: the data is basic geospatial information that the law requires to be open, it is not sold, and the source is credited. If a written grant is ever wanted, BIG answers data questions through its service desk.

## 4. How the codes and names line up with Kemendagri

niskora's official list of places is the Kemendagri code and data list. The current decree is **Kepmendagri 300.2.2-2430 Tahun 2025**, which amends 300.2.2-2138 Tahun 2025. A machine-readable copy is [cahyadsn/wilayah](https://github.com/cahyadsn/wilayah) (MIT licence, `db/wilayah.sql`, updated November 2026). Its 38 two-digit province rows were compared with the boundary sources.

| Kemendagri code and name (Kepmendagri 2025) | BIG June 2026 `KDPPUM` / `WADMPR` | BIG older layer `wadmpr` | geoBoundaries / HDX name | Natural Earth name (ISO) |
|---|---|---|---|---|
| 11 Aceh ... 96 Papua Barat Daya (all 38) | **identical codes and names, all 38** | identical except 31 | 34 names, no codes | 33, ISO codes |
| 19 Kepulauan Bangka Belitung | same | same | same | Bangka-Belitung (ID-BB) |
| 31 Daerah Khusus Ibukota Jakarta | same | **DKI Jakarta** | **Dki Jakarta** | **Jakarta Raya** (ID-JK) |
| 34 Daerah Istimewa Yogyakarta | same | same | same | **Yogyakarta** (ID-YO) |
| 65 Kalimantan Utara | same | same | same | **missing** |
| 93-96 four new Papua provinces | same | same | **missing** | **missing** |

What this means:

- **Join on the code, never on the name.** BIG's `KDPPUM` is the Kemendagri code itself. The BPS code columns (`KDPBPS`) are present but empty. At province level BPS and Kemendagri codes are the same two digits anyway, which is why HDX P-codes look like `ID11`.
- None of the files carries ISO 3166-2 codes except Natural Earth.
- Names still differ between datasets. Jakarta alone has four spellings across the files above, and its name is expected to change again when the capital moves (not checked against the law for this note). Codes stay stable through renames. This is one more reason to keep only the code in the map file and take display names from niskora's own Kemendagri list.

## 5. Producing the committed file

Measured run on 2026-11-16 (all in a scratch folder, never in the repo):

**Step 1 - download.** The service is slow and errors on large pages: a 50-feature page at full resolution is 55 MB, so the full layer is roughly 600 MB. Query one province at a time, let the server thin the lines to about 50 m (`maxAllowableOffset=0.0005` degrees, far finer than the about 8.5 km a pixel covers at 600 px), and retry until the reply is a FeatureCollection:

```sh
B="https://geoservices.big.go.id/rbi/rest/services/BATASWILAYAH/BATAS_KABKOTA_AR/MapServer/0/query"
for c in 11 12 13 14 15 16 17 18 19 21 31 32 33 34 35 36 51 52 53 61 62 63 64 65 \
         71 72 73 74 75 76 81 82 91 92 93 94 95 96; do
  until curl -s -m 900 "$B?where=KDPPUM%3D%27$c%27&outFields=KDPPUM&returnGeometry=true&outSR=4326&maxAllowableOffset=0.0005&geometryPrecision=5&f=geojson" \
          -o k$c.geojson && head -c 30 k$c.geojson | grep -q '"FeatureCollection"'; do sleep 5; done
done
```

Raw result: 14.7 MB of GeoJSON over 38 files, 541 regency polygons. Single downloads took from seconds to several minutes, and some failed with HTTP 500 before a retry worked. Running 8 in parallel finished all 38 in a few minutes.

**Step 2 - dissolve, simplify, write TopoJSON** with mapshaper:

```sh
npx mapshaper@0.6.102 -i k*.geojson combine-files \
  -merge-layers force name=provinces \
  -dissolve2 KDPPUM \
  -rename-fields code=KDPPUM \
  -clean \
  -simplify interval=2000 keep-shapes \
  -clean \
  -o provinces.topojson format=topojson quantization=10000
```

- `-dissolve2 KDPPUM` turns the regencies into 38 provinces and keeps only the code. Display names come from niskora's own Kemendagri list.
- The first `-clean` matters. The regency polygons have small gaps and overlaps along shared borders: mapshaper removed 8,293 slivers. Without this step, simplifying left 136 self-intersections it could not repair, and the file came out 60% larger.
- `interval=2000` removes detail below about 2 km, a quarter of a pixel at 600 px. The second `-clean` drops arcs left over from islands that collapsed.
- The output has one TopoJSON object, `provinces`, with 38 geometries. Each has a single property, **`code`** (string, `"11"` to `"96"`), which is the join key: colour each province from niskora's per-province figures by this code. In the React app, `topojson-client`'s `feature(topology, topology.objects.provinces)` turns the file into GeoJSON for `d3-geo`'s `geoPath`.

Sizes measured:

| Stage | Size | Polygons (province parts) |
|---|---|---|
| Downloaded regencies (about 50 m detail) | 14.7 MB GeoJSON | 541 regencies |
| Dissolved to 38 provinces and cleaned, before simplifying | 8.2 MB GeoJSON | 21,739 |
| Simplified, interval 1 km, TopoJSON | 152 KB (43 KB gzipped) | 613 |
| **Simplified, interval 2 km, TopoJSON** | **75 KB (23 KB gzipped)** | **321** |
| Simplified, interval 4 km, TopoJSON | 36 KB (12 KB gzipped) | 163 |
| For comparison: community 38-province TopoJSON | 49 KB | n/a |

Island survival at interval 2 km (polygons by area, before and after):

| Area | Before | After |
|---|---|---|
| 100 km² or more | 188 | 176 |
| 10 to 100 km² | 401 | 145 |
| 1 to 10 km² | 1,069 | 0 |
| under 1 km² | 20,081 | 0 |

`keep-shapes` keeps every **province** from vanishing, not every island. Why 12 polygons of 100 km² or more went missing was not investigated. Islands under about 10 km² are sub-pixel at 600 px (one pixel covers about 70 km²). If the map later grows or gets zoom, use `interval=1000` (about twice the size).

A preview rendered from the 2 km file at 600 px showed clean coastlines and all 38 provinces, including the six Papua provinces and Kalimantan Utara.

**Step 3 - commit** `provinces.topojson` and the attribution line from section 3. Note the BIG edition (June 2026) and the mapshaper command in the same place so the file can be rebuilt when Kemendagri or BIG change a boundary.

## 6. Open points

- **Disputed or provisional boundaries.** PP 45/2021 Pasal 10 ayat (5) says boundaries without a binding Kemendagri regulation are drawn as provisional. BIG's attributes do not flag which ones are provisional. For a province choropleth this does not matter.
- **BIG service reliability.** The REST service timed out often during this research (HTTP 500 on large queries). It is a one-off download, so this is acceptable, but do not let the app fetch from it at runtime.
- **Kemendagri list licence.** The cahyadsn/wilayah repo is MIT for its code and SQL. The data itself comes from a government decree. UU 28/2014 Pasal 42 says there is no copyright on "peraturan perundang-undangan", and Pasal 43 huruf b (section 3) covers anything the government publishes. A Kepmendagri is a decision rather than a regulation in the strict sense, so Pasal 43 is the safer basis. Either way, shipping the list looks fine.
