# Column pressure-drop publication

Status: In progress since 2026-10-27.

History:
- Removed the aggressive-path shortcut that published a zero-drop nominal pressure profile beside a positive marched hydraulic summary.
- Focused `slowGates --tests com.wormzjl.icheme.science.column.v3.V3TrayHydraulicsColumnTest` passed after the change.

Documents:
- [PRESSURE_DROP_REVIEW.md](PRESSURE_DROP_REVIEW.md)

Code areas:
- `src/main/java/com/wormzjl/icheme/science/column/v3/V3ColumnCalculator.java`

Open items: run the full verification lane before merging.
