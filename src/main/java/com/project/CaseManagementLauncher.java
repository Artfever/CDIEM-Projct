package com.project;

/**
 * Separate launcher used by IDEs and packaging tools that expect a plain main class.
 */
public final class CaseManagementLauncher {
    private CaseManagementLauncher() {
    }

    public static void main(String[] args) {
        CaseManagementApplication.main(args);
    }
}
