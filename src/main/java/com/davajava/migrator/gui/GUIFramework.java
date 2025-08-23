package com.davajava.migrator.gui;

public enum GUIFramework {
    TKINTER("tkinter", "Python Tkinter", "javax.swing"),
    PYGAME("pygame", "Python Pygame", "java.awt"),
    QT("PyQt5", "Python Qt", "javafx"),
    KIVY("kivy", "Python Kivy", "javafx"),
    GTK("gtk", "Python GTK", "javax.swing"),
    WPF("System.Windows", "C# WPF", "javafx"),
    WINFORMS("System.Windows.Forms", "C# WinForms", "javax.swing"),
    WIN32("windows.h", "C Win32 API", "java.awt"),
    COCOA("cocoa", "macOS Cocoa", "java.awt"),
    ELECTRON("electron", "JavaScript Electron", "javafx");

    private final String sourceIdentifier;
    private final String displayName;
    private final String targetFramework;

    GUIFramework(String sourceIdentifier, String displayName, String targetFramework) {
        this.sourceIdentifier = sourceIdentifier;
        this.displayName = displayName;
        this.targetFramework = targetFramework;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTargetFramework() {
        return targetFramework;
    }

    public static GUIFramework detectFromImport(String importStatement) {
        for (GUIFramework framework : values()) {
            if (importStatement.contains(framework.sourceIdentifier)) {
                return framework;
            }
        }
        return null;
    }
}