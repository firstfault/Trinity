package me.f1nal.trinity.gui.frames.impl.cp;

import me.f1nal.trinity.gui.components.filter.kind.IKind;
import me.f1nal.trinity.util.SearchTermMatchable;

public interface IBrowserViewerNode extends SearchTermMatchable, IKind {
    BrowserViewerNode getBrowserViewerNode();
}
