package org.openu.hyperltl.mc;

import java.util.List;

public record ReconstructionContext(
        List<QuantifierIntegrationStep> steps
) {
}