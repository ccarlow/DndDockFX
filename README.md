DndDockFX is yet another JavaFX docking library for JavaFX.

The motivation for this project emerged from the lack of power and flexibility of other JavaFX docking projects.

DragEvents are used instead of MouseEvents to mimic the docking behavior of other applications such as Chrome, Firefox, Gimp, etc.  Unlike MouseEvents, DragEvents are supported between windows.

Some docking projects use MouseEvents to handle dragging behavior and implement helper classes to manage dragging between windows.  That approach however falls short of DragEvents which leverage native OS window management which already handles dragging between not only windows of the running application but also windows outside of it.

This project is cross-platform unlike some docking projects that seem to fail on Linux more often than Windows.

This project uses Tabs as the drag control regardless of being docked in a TabPane or Splitpane or undocked in a separate window.  Using Tabs for all dock states enforces UI consistency and is consistent with other applications like Chrome, Firefox, Gimp, etc.  Some docking projects only use Tabs when docked in a TabPane and a different control when docked in a SplitPane or undocked as a separate windows which can cause confusion.

Draggable Tabs are not yet provided by JavaFX so the behavior in this project is a custom implementation.

The following issue is relate to implementing Draggable Tabs within JavaFX and its may effect the custom implementation of this project in the future:
https://bugs.openjdk.java.net/browse/JDK-8092098

Interestingly, the the issue above seems to describe partial implementation of a docking system but only for TabPanes.  This library expands on those ambitions to support SplitPanes for draggable tabs as well.

Another relevant source related to JavaFX Draggable Tabs:
https://choudhury.com/blog/2017/02/28/javafx-draggable-tabs/

Supports parenting and merging.

The following is a list of the JavaFX docking libraries that were tested and their shortcomings:

DockFX (https://github.com/RobertBColton/DockFX)
* Differentiates between DockPane and DockNode limiting flexibility of any window targeting any other window for docking.
* Weak support for multiple DockPanes.
* Stage.initOwner() must be used when using multiple DockPanes because dock behavior is controls by MouseEvents which are unaware of native window z-indexes and will show * DockPane popup buttons over native windows that are in front.
* Lacks window z-indexing that causes popup buttons of DockPanes in the background to appear over other DockPanes/DockNodes that are in front.
* Lacks controls for reopening DockNodes after they are closed

AnchorFX (https://github.com/alexbodogit/AnchorFX)
* Suffers the same z-indexing issues described for DockFX
* Dragging malfunctions on Linux
* Tabs don't support dragging which requires the tab to first be clicked before the window can be dragged

DromblerFX (https://github.com/Drombler/drombler-fx)
* Lacks drag and drop capabilities
* Differentiates beteween DockAreas and DockPanes.  DockAreas can only be represented as SplitPane items and cannot be reordered.  DockPanes can only exist within their designated DockArea and can only be represented as Tabs which cannot be reordered.

FXDock (https://github.com/andy-goryachev/FxDock, +1 for DragEvents over MouseEvents)
* Bloated codebase (350+ source files) much of which seems irrelevant to docking.
* Major bug with the dragged window not ignoring its own drag events
* On Linux the bug causes dragging behavior to malfunction
* On Window it allows a window to dock to itself causing ghost tabs and split items
