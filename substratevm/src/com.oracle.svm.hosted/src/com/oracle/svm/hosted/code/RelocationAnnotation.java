package com.oracle.svm.hosted.code;

import org.graalvm.compiler.asm.Assembler.CodeAnnotation;

import com.oracle.svm.hosted.image.RelocatableBuffer;

public abstract class RelocationAnnotation extends CodeAnnotation {
    protected RelocationAnnotation(int instructionStartPosition) {
        super(instructionStartPosition);
    }

    public abstract void patch(RelocatableBuffer buff, byte[] code);
}
