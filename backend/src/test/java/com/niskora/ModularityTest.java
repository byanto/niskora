package com.niskora;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.DiagramOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions.ElementsWithoutRelationships;

class ModularityTest {

  @Test
  void modulesKeepToTheirBoundariesAndTheDiagramIsCurrent() {
    ApplicationModules modules = ApplicationModules.of(NiskoraApplication.class);

    // Fails on a cycle and on any reach into another module's internals (ENG-43).
    modules.verify();

    // A module started alone in a use case test (ENG-33) still gets the clock and the error shape.
    assertThat(modules.getSharedModules())
        .extracting(module -> module.getIdentifier().toString())
        .containsExactly("common");

    // Regenerates the module diagram into docs/modules, cleaning out what was there (ENG-44).
    // The test runs in backend/, so the folder is one level up. A change to it is committed. A
    // module that depends on none and is used by none, such as common today, is still drawn.
    new Documenter(modules, Documenter.Options.defaults().withOutputFolder("../docs/modules"))
        .writeModulesAsPlantUml(
            DiagramOptions.defaults()
                .withElementsWithoutRelationships(ElementsWithoutRelationships.VISIBLE));
  }
}
