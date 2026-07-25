package me.f1nal.trinity.application;

/** Headless application boundary shared by presentation and transport adapters. */
public interface TrinityApplication {
    String version();

    TrinityStatus status();

    ProjectService projects();

    BrowseService browse();

    AnalysisService analysis();
    DexService dex();

    MutationService mutations();
}
