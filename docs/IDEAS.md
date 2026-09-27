# Ideas parked while building

One line each — things that tempted us mid-feature and were deliberately left out of scope.

- Cargo rockets: fluid cargo (a second, cargo-only tank on the Cargo Pad carried as a `(fluid, mB)` payload; needs a composite fluid view for the pipe seam).
- Cargo rockets: a `RouteEvents` hook for NeroEvents to delay or destroy a flight (space weather) — `FlightRecord.arrivesAt` would need to become mutable.
- Space elevators on top of the same `RouteRegistry` (an endpoint type that skips the fuel term).
- Satellites and asteroid fields — independent of routes; can follow.
- Cargo Pad access-list management from the GUI (today: owner + public flag; the list is API/erasure-ready but has no in-game editor).
