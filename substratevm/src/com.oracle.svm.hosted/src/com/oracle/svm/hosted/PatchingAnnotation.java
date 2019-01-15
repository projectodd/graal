package com.oracle.svm.hosted;

import org.graalvm.compiler.code.CompilationResult.CodeAnnotation;

import com.oracle.svm.hosted.image.RelocatableBuffer;

public abstract class PatchingAnnotation extends CodeAnnotation {

    protected PatchingAnnotation(int instructionStartPosition) {
        super(instructionStartPosition);
    }

    public abstract void patch(RelocatableBuffer reloc, int relative, byte[] code);

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
