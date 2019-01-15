package com.oracle.svm.hosted.amd64;

import org.graalvm.compiler.asm.amd64.AMD64BaseAssembler.OperandDataAnnotation;

import com.oracle.svm.hosted.PatchingAnnotation;
import com.oracle.svm.hosted.image.RelocatableBuffer;

public class AMD64PatchingAnnotation extends PatchingAnnotation {
    private final OperandDataAnnotation annotation;

    public AMD64PatchingAnnotation(int instructionStartPosition, OperandDataAnnotation annotation) {
        super(instructionStartPosition);
        this.annotation = annotation;
    }

    @Override
    public void patch(RelocatableBuffer reloc, int offset, byte[] code) {

    }
}
