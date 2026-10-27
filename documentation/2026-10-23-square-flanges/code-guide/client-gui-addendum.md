## Pipeline tool — branch addendum

**Snapshot:** 7bf50b7c (integrated into `claude/floating-roof-tanks-81d68b`), 2026-10-23. Refresh the main chapter snapshot when this batch reaches main.

`PipelineToolScreen` is a compact, slot-free `PipelineToolMenu` screen: one internal-diameter field in metres, a 0.001–1 m hint, Save and Cancel. It opens from Shift + right-click in air (`PipelineToolItem.use`) or against a block (`onItemUseFirst`). Enter saves; Escape cancels; invalid or non-finite values show an error and disable Save. Text editors consume inventory shortcuts through `ProcessUi.textKeyPressed`; resize preserves the draft.

The menu opening data carries hand, selected slot and the exact initial `PipelineToolSettings`. Save sends `PipelineToolNetwork.SavePayload(menuId, diameter)` on protocol `pipeline-tool-1`, then closes. The server requires the same live menu and exact held tool, validates the diameter again and syncs the changed component through the normal inventory menu. This screen displays only item settings: it requests no process state and creates no fluid subscription.

`icheme:pipeline_tool` is non-stackable, reusable and listed in Functional Blocks, with no recipe. Its tooltip shows the target and both controls; its handheld model reuses the vanilla shears sprite. Automated verification is the single `PipelineToolSettingsTest`; the owner handles the in-game check for this batch.

Owner follow-up: a pipe's inspection GUI opens only through the existing Fluid Debugger (`icheme:fluid_debugger`), retaining its existing debug mode. Ordinary pipe clicks pass through without opening a menu. The new Pipeline Tool continues to resize on right-click and open its own diameter settings on Shift + right-click; other fluid-device GUI interactions are unchanged.
