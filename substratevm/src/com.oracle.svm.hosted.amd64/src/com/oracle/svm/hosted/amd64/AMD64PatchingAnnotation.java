package com.oracle.svm.hosted.amd64;

import com.oracle.svm.core.SubstrateOptions;
import com.oracle.svm.core.graal.code.CGlobalDataReference;
import com.oracle.svm.core.util.VMError;
import jdk.vm.ci.code.site.ConstantReference;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.code.site.Reference;
import org.graalvm.compiler.asm.amd64.AMD64BaseAssembler.OperandDataAnnotation;

import org.graalvm.compiler.code.PatchingAnnotation;
import com.oracle.svm.hosted.image.RelocatableBuffer;

public class AMD64PatchingAnnotation extends PatchingAnnotation {
    private final OperandDataAnnotation annotation;

    public AMD64PatchingAnnotation(int instructionStartPosition, OperandDataAnnotation annotation) {
        super(instructionStartPosition);
        this.annotation = annotation;
    }

    @Override
    public void patch(Reference ref, RelocatableBuffer relocs, int compStart) {

        long siteOffset = compStart + annotation.operandPosition;
        if (ref instanceof DataSectionReference || ref instanceof CGlobalDataReference) {
            /*
             * Do we have an addend? Yes; it's constStart. BUT x86/x86-64 PC-relative
             * references are relative to the *next* instruction. So, if the next
             * instruction starts n bytes from the relocation site, we want to subtract n
             * bytes from our addend.
             */
            long addend = (annotation.nextInstructionPosition - annotation.operandPosition);
            relocs.addPCRelativeRelocationWithAddend((int) siteOffset, annotation.operandSize, addend, ref);
        } else if (ref instanceof ConstantReference) {
            assert SubstrateOptions.SpawnIsolates.getValue() : "Inlined object references must be base-relative";
            relocs.addDirectRelocationWithoutAddend((int) siteOffset, annotation.operandSize, ref);
        } else {
            throw VMError.shouldNotReachHere("Unknown type of reference in code");
        }

    }

    public void patch(int codePos, int relative, byte[] code) {
        int offset = relative - (annotation.nextInstructionPosition - annotation.instructionPosition);

        int curValue = offset;
        for (int i = 0; i < annotation.operandSize; i++) {
            assert code[annotation.operandPosition + i] == 0;
            code[annotation.operandPosition + i] = (byte) (curValue & 0xFF);
            curValue = curValue >>> 8;
        }
        assert curValue == 0;
    }
}
