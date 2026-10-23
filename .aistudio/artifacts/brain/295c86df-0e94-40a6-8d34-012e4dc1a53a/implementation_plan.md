# AeroPulse: Breath, Allergy & Runner Radar

AeroPulse is an atmospheric and environmental intelligence radar for runners, allergy sufferers, parents with young children, and pet walkers. It aggregates Open-Meteo air quality (PM2.5, PM10, AQI, UV index, pollen counts), sunrise/sunset golden hours, and NOAA space weather (geomagnetic planetary Kp-index and solar activity) to compute a real-time, profile-tailored outdoor "Go / No-Go" window. It also features an interactive Wear OS companion preview and watch tile simulator for quick wrist glances.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification and are locked into the application specification:

- **Confirmed Decision 1 (User Profiles)**: Multi-profile toggle supporting **Runners & Athletes**, **Allergy & Respiratory Sufferers**, **Parents & Strollers**, and **Pet Walkers**, each with customized threshold sensitivity for PM2.5, ozone, UV, and allergen species (grass, ragweed, birch, alder).
- **Confirmed Decision 2 (Wear OS Presentation)**: High-fidelity interactive **Wear OS Watch Face & Tile Simulator** directly inside the Android app, rendering an authentic circular smartwatch display with real-time complication updates, quick Go/No-Go status ring, and tile swipe interactions.
- **Confirmed Decision 3 (Location Services)**: Hybrid location model featuring **Device GPS auto-detection** paired with **instant City Search** (geocoding) and **Saved Favorite Spots** persisted locally via Room Database with quick 1-tap switching.
- **Recommended Default (Visual Aesthetic)**: Deep Atmospheric Aerodynamic Theme — obsidian deep navy (`#0B101B`), aero-cyan active glow (`#00F2FE`), amber warning tiers (`#FFB300`), and clean M3 pill controls with edge-to-edge typography.

---

### 1. Overview & Core Concept

- **What It Does**: AeroPulse transforms dense atmospheric and space telemetry into actionable outdoor decisions. It delivers an instant, animated **Go / Caution / No-Go status dial** with safe running & walking hour windows, detailed pollutant breakdowns (PM2.5, PM10, O3, NO2, Dust), botanical pollen radars (Grass, Ragweed, Birch, Alder, Mugwort, Olive), UV solar exposure meters with countdown to sunset, and NOAA Space Weather geomagnetic alerts.
- **Target Audience / Personas**:
  - *Endurance Runners & Cyclists*: Seek optimal aerobic windows free of fine particulate matter (PM2.5) spikes and extreme heat/UV.
  - *Allergy & Asthma Sufferers*: Monitor specific allergen pollen species spikes and air stagnation.
  - *Parents & Caregivers*: Verify clean air and safe UV hours before outdoor playground or stroller time.
  - *Pet Walkers*: Check asphalt-temperature/UV index and air quality for safe dog walking.
- **Key Value**: Replaces checking 4 different weather, air quality, pollen, and UV apps with one decisive "Run Radar" dial and hourly timeline.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Radar Dashboard (Main Screen)**:
   - Dynamic Top Header displaying current city/location name, GPS status indicator, and quick location switcher.
   - Profile Selector Chip Carousel (Runners, Allergy, Parents, Pet Walkers) that immediately recalibrates the Go/No-Go score and thresholds with smooth spring animations.
   - Large Circular "AeroPulse Verdict Dial": Clear color-coded score (0-100), status badge ("OPTIMAL WINDOW - GO", "MODERATE CAUTION", "HAZARDOUS - STAY INDOORS"), and primary restricting factor (e.g. "PM2.5 spike expected at 3 PM" or "Severe Ragweed").
   - 24-Hour "Outdoor Safe Window" Scroller: Hourly horizontal timeline with color-coded safety bars, hourly temperature, PM2.5, and UV.
   - Deep Dive Telemetry Cards:
     - *Air Quality Matrix*: PM2.5, PM10, US AQI, European AQI, Ozone, NO2.
     - *Botanical Pollen Radar*: Grass, Ragweed, Birch, Alder breakdown with botanical allergen risk gauges.
     - *Solar & Celestial Rhythm*: Current UV index, maximum UV time, golden hour, and sunrise/sunset arc.
     - *NOAA Space Weather*: Planetary Kp-index gauge (0-9 scale), geomagnetic storm severity (G0-G5), and HF radio/aurora outlook.
2. **Interactive Wear OS Simulator Screen**:
   - Realistic circular smartwatch casing with bevel and active bezel.
   - Watch Face mode: Shows time, heart rate indicator mock, Go/No-Go color halo, and current PM2.5/UV complications.
   - Interactive Watch Tile mode: Swipeable circular tiles simulating the Wear OS AeroPulse Tile with 1-tap "Can I run now?" check.
3. **Saved Locations & Search Modal**:
   - Fast geocoding search for worldwide cities.
   - Local saved spots (e.g., "Home Trail", "Central Park", "Office") with stored coordinates and quick deletion.
4. **Custom Sensitivity & Threshold Settings**:
   - Fine-tune personal sensitivity sliders for PM2.5 threshold, UV warning level, and pollen alert triggers.

#### Visual Identity & Theme
- **Aesthetic Direction**: High-tech athletic observatory, inspired by aerospace flight computers and clean sports tracking.
- **Color Palette**:
  - Background: Deep Void Navy `#080C14`
  - Surface / Card: Obsidian Blue `#101726` with subtle 1dp border `#1E293B`
  - Brand Primary / Go Glow: Aero Cyan `#00E5FF` and Mint Green `#10B981`
  - Caution / Warning: Solar Amber `#F59E0B`
  - Danger / No-Go: Hazard Crimson `#EF4444`
  - Text: Clean Crisp White `#F8FAFC` and Muted Steel `#94A3B8`
- **Typography**: Clean high-legibility sans-serif with bold tabular figures for numbers, index metrics, and timestamps.
- **Component Styling & Layout**: Edge-to-edge layout with full top status bar insets and bottom navigation bar insets, glassmorphic tonal cards, and responsive adaptive layouts for phones and tablets.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Open-Meteo & NOAA REST APIs with Offline Cache**
  - *Chosen Approach*: Retrofit-driven clients connecting to Open-Meteo Air Quality & Weather Forecast APIs and NOAA SWPC Space Weather endpoints, combined with Room Database local cache.
  - *Why*: Open-Meteo requires no API key, provides open European and US AQI, pollen counts, and solar forecast. NOAA SWPC is publicly available and reliable. Room ensures instantaneous app startup even without network connectivity.
  - *Alternatives Considered*: Hardcoded mock data (rejected per guidelines), single aggregate weather API (lacks specialized pollen and space weather indices).

- **Decision 2: Profile-Aware Multi-Factor Algorithm for "Go / No-Go" Score**
  - *Chosen Approach*: A weighted scoring engine that computes an index from 0 to 100 based on the active user profile's sensitivity weights. For runners, PM2.5 and Ozone are weighted heavily; for allergy sufferers, specific pollen counts dominate; for parents/pets, UV index and heat/particulates dominate.
  - *Why*: A runner might tolerate moderate pollen if air is crisp, but an asthma sufferer cannot; conversely, midday high UV stops parents with toddlers while runners might run at twilight.

- **Decision 3: Wear OS Companion Simulator inside Mobile App**
  - *Chosen Approach*: Implement a custom Compose circular watch viewport component that renders authentic Wear OS Watch Face complications and Tile layouts using the same live data engine.
  - *Why*: Allows immediate testing and visualization of the Wear OS experience directly within the Android mobile app preview emulator.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                              │
│                                                                        │
│   ┌────────────────────────┐         ┌──────────────────────────────┐  │
│   │   AeroPulseDashboard   │         │     WearOsSimulatorScreen    │  │
│   │  (Dial, Window, Cards) │         │   (Watch Face & Tile Views)  │  │
│   └───────────┬────────────┘         └──────────────┬───────────────┘  │
│               │                                     │                  │
│               └───────────────┬─────────────────────┘                  │
│                               ▼                                        │
│                      AeroPulseViewModel                                │
│        (StateFlow<AeroPulseUiState>, Active Profile, Window Filter)    │
└───────────────────────────────┬────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          Domain / Scoring                              │
│                                                                        │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                     GoNoGoEngine                                │  │
│   │   Calculates 0-100 score, limiting factor, and safe windows     │  │
│   └─────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          Data & Network                                │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                   AeroPulseRepository                          │   │
│   └──────┬──────────────────────┬──────────────────────────┬───────┘   │
│          ▼                      ▼                          ▼           │
│   OpenMeteoAirQualityApi    OpenMeteoWeatherApi     NoaaSpaceWeatherApi│
│   (PM2.5, Pollen, UV, AQI) (Temp, Sunrise, Sunset) (Planetary Kp)      │
│          ▲                                                             │
│          │                                                             │
│   ┌──────┴─────────────────────────────────────────────────────────┐   │
│   │                       Room Database                            │   │
│   │     (Saved Locations, Profile Settings, Cached Radar Snapshot)  │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

#### Data Entities & State Model
1. **`SavedLocation` (Room Entity)**: `id`, `name`, `latitude`, `longitude`, `country`, `isFavorite`, `lastUpdated`.
2. **`UserProfile` (Enum / Model)**: `Runner`, `AllergySufferer`, `ParentStroller`, `PetWalker`, with threshold configs.
3. **`AeroPulseSnapshot`**:
   - Current: PM2.5, PM10, AQI (US & EU), O3, NO2, Grass Pollen, Ragweed Pollen, Birch Pollen, UV Index, Temp, Humidity, Sunrise/Sunset, NOAA Kp-Index.
   - HourlyForecast: 24-hour list of timestamps, PM2.5, UV, Pollen, and Go/No-Go score.
   - Verdict: `VerdictLevel` (Go / Caution / NoGo), `score` (0..100), `summaryTitle`, `headlineReason`, `bestWindowTimeSpan`.

#### Interactive Component & State Mapping
- Profile chip tap -> Updates `activeProfile` in ViewModel -> Recomputes Verdict & hourly windows immediately.
- Location search -> Triggers Open-Meteo Geocoding API -> Selects location -> Saves to Room -> Fetches fresh telemetry.
- Wear OS mode toggle -> Switches between Mobile Dashboard, Wear OS Watch Face Preview, and Wear OS Tile Simulator.
- Refresh action -> Pulls live remote data with loading spinner and offline fallback banner on failure.
