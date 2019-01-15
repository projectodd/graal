package com.oracle.svm.core.graal.code;

import java.util.HashMap;
import java.util.Map;

import org.graalvm.compiler.code.CompilationResult;
import org.graalvm.compiler.code.PatchingAnnotation;
import org.graalvm.compiler.lir.asm.CompilationResultBuilder;

public class InstructionPatcher {


    private final Map<Integer, PatchingAnnotation> annotations;

    public InstructionPatcher(CompilationResult compilationResult) {
        /*
         * The AMD64Assembler emits additional information for instructions that describes the
         * location of the displacement in addresses and the location of the immediate operand for
         * calls that we need to patch.
         */
        annotations = new HashMap<>();
        for (CompilationResult.CodeAnnotation codeAnnotation : compilationResult.getAnnotations()) {
            if (codeAnnotation instanceof CompilationResultBuilder.AssemblerAnnotation) {
                if (codeAnnotation instanceof PatchingAnnotation) {
                    PatchingAnnotation patchingAnnotation = (PatchingAnnotation) codeAnnotation;
                    annotations.put(patchingAnnotation.position, patchingAnnotation);
                }
            }
        }
    }

    public PatchingAnnotation getPatching(int codePos) {
        return annotations.get(codePos);
    }

}
